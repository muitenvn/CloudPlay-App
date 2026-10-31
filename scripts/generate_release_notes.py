#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generates a diff-based APK release summary and packages the APK for GitHub Releases.
- Reads versionName & versionCode from Gradle's output-metadata.json
- Inspects git diff (prev_tag..HEAD) and recent commits
- Summarizes actual changes from the current diff with a local fallback when no API key is set
"""

import datetime
import glob
import hashlib
import json
import os
import re
import shutil
import subprocess
import urllib.parse
import urllib.request


def run_cmd(cmd: list[str]) -> str:
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=False)
        return res.stdout.strip()
    except Exception:
        return ""


def resolve_version_info(run_number: str) -> tuple[str, str]:
    app_ver = ""
    app_code = ""

    meta_path = "app/build/outputs/apk/debug/output-metadata.json"
    if os.path.isfile(meta_path):
        try:
            with open(meta_path, "r", encoding="utf-8") as f:
                meta = json.load(f)
            elements = meta.get("elements") or []
            if elements:
                app_ver = str(elements[0].get("versionName", "")).strip()
                app_code = str(elements[0].get("versionCode", "")).strip()
        except Exception:
            pass

    # Fallback if metadata was missing or contained unresolved template placeholders
    if not app_ver or "${" in app_ver:
        app_ver = f"1.2.{run_number}"
    if not app_code or not app_code.isdigit():
        app_code = str(100 + int(run_number) if run_number.isdigit() else 103)

    return app_ver, app_code


def cleanup_malformed_github_releases(repo: str, gh_token: str) -> None:
    """Deletes any previous malformed releases/tags containing '${runNumber}' on GitHub."""
    if not repo or not gh_token:
        return
    headers = {
        "Authorization": f"Bearer {gh_token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "CloudPlay-Release-Bot",
    }
    try:
        req = urllib.request.Request(
            f"https://api.github.com/repos/{repo}/releases?per_page=30",
            headers=headers,
        )
        with urllib.request.urlopen(req, timeout=15) as resp:
            releases = json.loads(resp.read().decode("utf-8"))
        for rel in releases:
            tag = rel.get("tag_name", "")
            rel_id = rel.get("id")
            if "${" in tag and rel_id:
                try:
                    del_req = urllib.request.Request(
                        f"https://api.github.com/repos/{repo}/releases/{rel_id}",
                        headers=headers,
                        method="DELETE",
                    )
                    urllib.request.urlopen(del_req, timeout=10).close()
                except Exception:
                    pass
                try:
                    encoded_tag = urllib.parse.quote(tag, safe="")
                    del_tag_req = urllib.request.Request(
                        f"https://api.github.com/repos/{repo}/git/refs/tags/{encoded_tag}",
                        headers=headers,
                        method="DELETE",
                    )
                    urllib.request.urlopen(del_tag_req, timeout=10).close()
                except Exception:
                    pass
    except Exception:
        pass


def extract_newly_added_changelog_bullets(prev_tag: str) -> list[str]:
    """
    Extracts bullet points from CHANGELOG.md ONLY if they were newly added ('+')
    in the diff between prev_tag and HEAD. Never returns stale bullets from older releases.
    """
    diff_cmd = (
        ["git", "diff", "-U0", prev_tag, "HEAD", "--", "CHANGELOG.md"]
        if prev_tag
        else ["git", "show", "-U0", "--pretty=", "HEAD", "--", "CHANGELOG.md"]
    )
    diff_out = run_cmd(diff_cmd)
    if not diff_out:
        return []

    bullets: list[str] = []
    for raw_line in diff_out.splitlines():
        if not raw_line.startswith("+") or raw_line.startswith("+++"):
            continue
        line = raw_line[1:].strip()
        if line.startswith("- **"):
            m = re.match(r"^-\s*\*\*([^*]+)\*\*:?\s*(.+)", line)
            if m:
                title = m.group(1).strip().rstrip(":")
                desc = m.group(2).strip()
                desc = re.sub(r"\s*\(`[^)]+\)", "", desc).strip()
                if not desc.endswith("."):
                    desc += "."
                bullets.append(f"- **{title}:** {desc}")
            else:
                bullets.append(line)
    return bullets[:6]


def extract_real_git_diff_signals(prev_tag: str) -> tuple[str, list[str], list[str], list[str]]:
    """
    Inspects the actual git diff between prev_tag and HEAD to extract:
    1. Raw added code/comment/UI lines (filtered for high signal)
    2. Added/modified function & class names
    3. Added descriptive Vietnamese/English comments & docstrings
    4. Added/updated user-facing UI strings
    """
    diff_cmd = (
        ["git", "diff", "-U0", prev_tag, "HEAD"]
        if prev_tag
        else ["git", "show", "-U0", "--pretty=", "HEAD"]
    )
    raw_diff = run_cmd(diff_cmd)
    if not raw_diff:
        return "", [], [], []

    functions_touched: list[str] = []
    comments_added: list[str] = []
    ui_strings_added: list[str] = []
    compact_diff_lines: list[str] = []
    current_file = ""

    for line in raw_diff.splitlines():
        if line.startswith("+++ b/"):
            current_file = line[6:].strip()
            compact_diff_lines.append(f"\n[File: {current_file}]")
            continue
        if not line.startswith("+") or line.startswith("+++"):
            continue

        added = line[1:].strip()
        if not added or len(added) < 4:
            continue

        # Skip pure imports or closing braces
        if added.startswith("import ") or added.startswith("package ") or added in ("}", "};", "),", ")"):
            continue

        # Capture Kotlin / JS functions or composables
        fn_match = re.search(
            r"(?:fun\s+([A-Za-z0-9_]+)|window\.(__[A-Za-z0-9_]+)\s*=|function\s+([A-Za-z0-9_]+)\s*\()",
            added,
        )
        if fn_match:
            fn_name = fn_match.group(1) or fn_match.group(2) or fn_match.group(3)
            if fn_name and fn_name not in functions_touched:
                functions_touched.append(f"{os.path.basename(current_file)}:{fn_name}")

        # Capture meaningful comments (especially Vietnamese or technical explanations)
        comment_match = re.match(r"^(?://+|\*+)\s*(.+)", added)
        if comment_match:
            c_text = comment_match.group(1).strip().strip("=").strip("-").strip()
            if len(c_text) >= 15 and not c_text.startswith("http") and c_text not in comments_added:
                comments_added.append(c_text)

        # Capture meaningful Vietnamese UI strings
        for str_match in re.finditer(r'"([^"\n]{12,140})"', added):
            s_val = str_match.group(1).strip()
            # Check if it contains Vietnamese characters or clear feature keywords
            if re.search(r"[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ]", s_val):
                if s_val not in ui_strings_added and "${" not in s_val and "http" not in s_val:
                    ui_strings_added.append(s_val)

        if len(compact_diff_lines) < 160 and len(added) <= 180:
            compact_diff_lines.append(f"+ {added}")

    compact_diff_str = "\n".join(compact_diff_lines)[:3500]
    return compact_diff_str, functions_touched[:20], comments_added[:18], ui_strings_added[:18]


def analyze_git_diff_smart_bullets(
    app_ver: str,
    raw_files: list[str],
    commits: list[str],
    functions_touched: list[str],
    comments_added: list[str],
    ui_strings_added: list[str],
    compact_diff: str,
) -> list[str]:
    """
    Deterministic semantic analyzer that builds accurate, specific Vietnamese release bullets
    directly from the actual code changes in prev_tag..HEAD (never repeating unrelated old releases).
    """
    smart_bullets: list[str] = []
    diff_lower = (compact_diff + "\n" + "\n".join(comments_added) + "\n" + "\n".join(ui_strings_added)).lower()
    files_joined = " ".join(raw_files).lower()

    # 1. Check for Video Recording (.MP4 container, stereo AAC/PCM audio, Full Screen vs 16:9)
    if (
        "zerolagscreenrecorder" in files_joined
        or "onscreenrecordaudiopcm" in diff_lower
        or "mimetype_audio_aac" in diff_lower
        or "__startliveaudiopcmtap" in diff_lower
        or "isgenuinemp4file" in diff_lower
    ):
        smart_bullets.append(
            "- **Sửa lỗi quay màn hình và âm thanh:** "
            "Thu âm thanh từ luồng game qua AAC-LC & H.264 và lưu video dưới định dạng `.mp4`."
        )

    # 2. Check for Anti-Stutter / Preserving Live Gameplay Screen Aspect Ratio
    if (
        "__applyvideoaspectandcolorcss" in diff_lower
        or "isdefaultfit" in diff_lower
        or "__setrecordingperformancemode" in diff_lower
        or "không thay đổi màn hình" in diff_lower
        or "giữ nguyên khung hình" in diff_lower
    ):
        smart_bullets.append(
            "- **Giữ nguyên khung hình khi đang chơi:** "
            "Tránh thay đổi lại thuộc tính CSS của thẻ `<video>` trong lúc đang phát luồng game hoặc khi bấm quay màn hình."
        )

    # 3. Check for Priority Reordering in HUD / In-Game Quick Settings / Settings Screen
    if (
        "floatinghud.kt" in files_joined
        or "cloudbrowserscreen.kt" in files_joined
        or "settingsscreen.kt" in files_joined
        or "homescreen.kt" in files_joined
    ):
        if "tính năng phụ" in diff_lower or "onToggleGpuLoadReduction".lower() in diff_lower or "in_game_quick_settings_dialog" in diff_lower:
            smart_bullets.append(
                "- **Sắp xếp lại giao diện và mục Cài đặt:** "
                "Đưa các tùy chọn độ phân giải, khung hình (FPS) và chế độ máy yếu lên đầu; gộp gọn các mục ít dùng."
            )

    # 4. Check for Clarity Boost / Adaptive Bitrate / WebView Performance bug fixes
    if (
        "adaptivequalitymanager.kt" in files_joined
        or "__toggleclarityboost" in diff_lower
        or "injectedforcurrentpage" in diff_lower
        or "savednextsessionmbps" in diff_lower
    ):
        smart_bullets.append(
            "- **Sửa lỗi WebView và bộ lọc hình ảnh:** "
            "Khắc phục lỗi bộ lọc làm nét tự bật lại khi tải trang và tránh nạp lặp lại script trong WebView."
        )

    # 5. Check for Release Notes / CI workflow improvements
    if "generate_release_notes.py" in files_joined or "build-and-release-apk.yml" in files_joined:
        smart_bullets.append(
            "- **Cập nhật kịch bản tạo ghi chú phát hành:** "
            "Tóm tắt ngắn gọn các thay đổi thực tế từ `git diff` và lịch sử commit của bản dựng."
        )

    # 6. Check for VPN / Proxy / Region Unlock changes if touched
    if any(k in files_joined for k in ("vpnmanager", "xboxcloudvpnservice", "geoipdetector", "serversscreen")):
        smart_bullets.append(
            "- **Cập nhật phần kết nối máy chủ mở khóa vùng:** "
            "Cải thiện kiểm tra kết nối proxy/VPN và tự tắt chuyển tiếp sau khi vào game."
        )

    # 7. Check for Virtual Gamepad / Controls changes if touched
    if "virtualgamepad.kt" in files_joined or "gamepaddetector.kt" in files_joined:
        smart_bullets.append(
            "- **Cập nhật tay cầm ảo trên màn hình:** "
            "Chỉnh lại bố cục nút bấm, độ nhạy cần điều khiển và tùy chọn đặt lại vị trí mặc định."
        )

    # If none of the specific domain detectors matched, build bullets from added comments / UI strings / commits
    if len(smart_bullets) < 2:
        for c in comments_added[:3]:
            if len(c) > 20 and re.search(r"[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ]", c):
                clean_c = c.rstrip(".") + "."
                bullet = f"- **Thay đổi trong mã nguồn:** {clean_c}"
                if bullet not in smart_bullets:
                    smart_bullets.append(bullet)

    if not smart_bullets and commits:
        for cm in commits[:4]:
            # Strip commit hash prefix `- `abc1234` `
            clean_cm = re.sub(r"^-\s*`[0-9a-f]+`\s*", "- ", cm)
            smart_bullets.append(clean_cm)

    if not smart_bullets:
        smart_bullets.append(
            f"- **Bản dựng v{app_ver}:** Sửa lỗi nhỏ và cập nhật giao diện."
        )

    return smart_bullets[:5]


def is_valid_ai_summary(text: str, compact_diff: str) -> bool:
    """
    Validates that the AI-generated summary:
    1. Contains at least 2 bullet points starting with '-' or '*'
    2. Does NOT hallucinate old Fortnite 8ms/Opus/Claw features unless those terms actually appear in compact_diff
    """
    if not text or len(text.strip()) < 30:
        return False
    lines = [ln.strip() for ln in text.strip().splitlines() if ln.strip()]
    bullet_lines = [ln for ln in lines if ln.startswith("-") or ln.startswith("*") or ln.startswith("•")]
    if len(bullet_lines) < 2:
        return False

    # Guard against repeating old v1.3.0 Fortnite release notes if they aren't in the current diff
    stale_phrases = ["8ms zero-delay", "128kbps cbr opus", "claw 4 ngón", "cảm ứng epic"]
    diff_lower = compact_diff.lower()
    text_lower = text.lower()
    for phrase in stale_phrases:
        if phrase in text_lower and phrase not in diff_lower:
            return False

    return True


def generate_ai_description(
    app_ver: str,
    prev_tag: str,
    raw_files: list[str],
    commits: list[str],
) -> str:
    """
    Calls Gemini (2.5-flash / 2.5-flash-lite / 2.0-flash) or GitHub Models (gpt-4o-mini)
    using the REAL git diff of prev_tag..HEAD, with a deterministic diff-analyzer fallback.
    """
    newly_added_changelog = extract_newly_added_changelog_bullets(prev_tag)
    compact_diff, functions_touched, comments_added, ui_strings_added = extract_real_git_diff_signals(prev_tag)
    smart_fallback_bullets = analyze_git_diff_smart_bullets(
        app_ver=app_ver,
        raw_files=raw_files,
        commits=commits,
        functions_touched=functions_touched,
        comments_added=comments_added,
        ui_strings_added=ui_strings_added,
        compact_diff=compact_diff,
    )

    context_parts = []
    if commits:
        context_parts.append("Commits trong bản này:\n" + "\n".join(commits[:6]))
    if raw_files:
        context_parts.append("Các file thay đổi:\n" + "\n".join(f"- {f}" for f in raw_files[:12]))
    if functions_touched:
        context_parts.append("Các hàm/module vừa sửa:\n" + ", ".join(functions_touched[:15]))
    if newly_added_changelog:
        context_parts.append("Ghi chú mới thêm vào CHANGELOG:\n" + "\n".join(newly_added_changelog[:5]))
    if comments_added:
        context_parts.append("Ghi chú kỹ thuật trong code mới:\n" + "\n".join(f"- {c}" for c in comments_added[:10]))
    if ui_strings_added:
        context_parts.append("Chuỗi giao diện mới/sửa đổi:\n" + "\n".join(f"- {s}" for s in ui_strings_added[:10]))
    if compact_diff:
        context_parts.append("Trích đoạn Git Diff thực tế (+ lines):\n" + compact_diff[:2200])

    full_context = "\n\n".join(context_parts)[:3800]

    system_instruction = (
        "Bạn là người viết ghi chú phát hành (release notes) cho ứng dụng Android CloudPlay. "
        "Hãy viết 3 đến 5 gạch đầu dòng bằng Tiếng Việt (dạng `- **Tiêu đề ngắn:** Nội dung.`) "
        "liệt kê các thay đổi và sửa lỗi có trong Git Diff được cung cấp.\n"
        "QUY TẮC:\n"
        "1. Chỉ nêu đúng những thay đổi có trong Git Diff và Commit của bản này.\n"
        "2. Không lặp lại tính năng từ các phiên bản cũ nếu không sửa trong lần này.\n"
        "3. Dùng văn phong bình thường, trung thực, khiêm tốn; tuyệt đối không dùng từ ngữ quảng cáo hay phóng đại (như 'đỉnh cao', 'tuyệt đối', 'siêu tốc', 'độc quyền', 'thông minh', 'chuẩn quốc tế')."
    )

    user_prompt = (
        f"Hãy tóm tắt ngắn gọn (3-5 gạch đầu dòng Tiếng Việt) các thay đổi trong bản "
        f"CloudPlay v{app_ver} (so với {prev_tag or 'bản trước'}) dựa trên dữ liệu sau:\n\n"
        f"{full_context}"
    )

    gemini_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not gemini_key and os.path.isfile(".env"):
        try:
            with open(".env", "r", encoding="utf-8") as f:
                for line in f:
                    if line.strip().startswith("GEMINI_API_KEY="):
                        val = line.strip().split("=", 1)[1].strip().strip('"').strip("'")
                        if val and val != "MY_GEMINI_API_KEY":
                            gemini_key = val
        except Exception:
            pass

    gh_token = (
        os.environ.get("GH_MODELS_TOKEN", "").strip()
        or os.environ.get("GITHUB_TOKEN", "").strip()
    )

    # 1. Try Gemini Models in order of accuracy & availability
    if gemini_key and gemini_key != "MY_GEMINI_API_KEY":
        for gemini_model in (
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-3.1-flash-lite-preview",
        ):
            try:
                url = (
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                    f"{gemini_model}:generateContent?key={gemini_key}"
                )
                payload = {
                    "systemInstruction": {"parts": [{"text": system_instruction}]},
                    "contents": [{"parts": [{"text": user_prompt}]}],
                    "generationConfig": {
                        "temperature": 0.2,
                        "maxOutputTokens": 420,
                    },
                }
                req = urllib.request.Request(
                    url,
                    data=json.dumps(payload).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                with urllib.request.urlopen(req, timeout=20) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    text = data["candidates"][0]["content"]["parts"][0]["text"].strip()
                    if is_valid_ai_summary(text, compact_diff):
                        return text
            except Exception:
                pass

    # 2. Try GitHub Models API via built-in GITHUB_TOKEN (with models: read permission)
    if gh_token:
        for endpoint, model_id in (
            ("https://models.github.ai/inference/chat/completions", "openai/gpt-4o-mini"),
            ("https://models.github.ai/inference/chat/completions", "openai/gpt-4o"),
            ("https://models.inference.ai.azure.com/chat/completions", "gpt-4o-mini"),
            ("https://models.inference.ai.azure.com/chat/completions", "gpt-4o"),
        ):
            try:
                payload = {
                    "model": model_id,
                    "messages": [
                        {"role": "system", "content": system_instruction},
                        {"role": "user", "content": user_prompt},
                    ],
                    "temperature": 0.2,
                    "max_tokens": 420,
                }
                req = urllib.request.Request(
                    endpoint,
                    data=json.dumps(payload).encode("utf-8"),
                    headers={
                        "Content-Type": "application/json",
                        "Accept": "application/vnd.github+json",
                        "X-GitHub-Api-Version": "2022-11-28",
                        "User-Agent": "CloudPlay-Release-Bot",
                        "Authorization": f"Bearer {gh_token}",
                    },
                    method="POST",
                )
                with urllib.request.urlopen(req, timeout=18) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    text = data["choices"][0]["message"]["content"].strip()
                    if is_valid_ai_summary(text, compact_diff):
                        return text
            except Exception:
                pass

    # 3. Try keyless OpenAI-compatible AI endpoint if tokens were unavailable or rate-limited
    try:
        payload = {
            "model": "openai",
            "messages": [
                {"role": "system", "content": system_instruction},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": 0.2,
        }
        req = urllib.request.Request(
            "https://text.pollinations.ai/openai",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "User-Agent": "CloudPlay-Release-Bot",
            },
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=18) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            text = data["choices"][0]["message"]["content"].strip()
            if is_valid_ai_summary(text, compact_diff):
                return text
    except Exception:
        pass

    # 4. If newly added CHANGELOG bullets exist in THIS commit's git diff, prefer them combined with smart diff bullets
    if newly_added_changelog:
        combined = list(newly_added_changelog)
        for b in smart_fallback_bullets:
            if len(combined) < 5 and b not in combined:
                combined.append(b)
        return "\n".join(combined[:5])

    # 5. Accurate deterministic Git-Diff Analyzer fallback (never returns stale CHANGELOG.md bullets)
    return "\n".join(smart_fallback_bullets)


def main() -> None:
    os.makedirs("release-output", exist_ok=True)
    apk_candidates = sorted(glob.glob("app/build/outputs/apk/debug/*.apk"))
    if not apk_candidates:
        raise SystemExit("Error: APK file not found in app/build/outputs/apk/debug/")
    apk_file = apk_candidates[0]

    run_number = os.environ.get("GITHUB_RUN_NUMBER", "").strip() or "3"
    app_ver, app_code = resolve_version_info(run_number)

    github_ref = os.environ.get("GITHUB_REF", "")
    github_ref_name = os.environ.get("GITHUB_REF_NAME", "")
    if github_ref.startswith("refs/tags/") and github_ref_name and "${" not in github_ref_name:
        tag_name = github_ref_name
        release_name = f"CloudPlay Xbox {tag_name} (Build #{run_number})"
    else:
        tag_name = f"v{app_ver}"
        release_name = f"CloudPlay Xbox v{app_ver} (Build #{run_number})"

    github_output = os.environ.get("GITHUB_OUTPUT", "")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as out:
            out.write(f"tag_name={tag_name}\n")
            out.write(f"release_name={release_name}\n")

    apk_ver_name = f"CloudPlay-Xbox-v{app_ver}.apk"
    shutil.copy2(apk_file, os.path.join("release-output", apk_ver_name))
    shutil.copy2(apk_file, os.path.join("release-output", "app-debug.apk"))

    # Compute size and SHA-256
    size_mb = os.path.getsize(apk_file) / (1024 * 1024)
    apk_size_str = f"{size_mb:.1f} MB"
    sha256_hash = hashlib.sha256()
    with open(apk_file, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            sha256_hash.update(chunk)
    apk_sha256 = sha256_hash.hexdigest()

    tz_vn = datetime.timezone(datetime.timedelta(hours=7))
    build_date = datetime.datetime.now(tz_vn).strftime("%d/%m/%Y %H:%M:%S (GMT+7)")
    latest_hash = run_cmd(["git", "rev-parse", "--short", "HEAD"]) or "latest"

    # Clean up any old malformed '${runNumber}' releases/tags on GitHub
    cleanup_malformed_github_releases(
        os.environ.get("GITHUB_REPOSITORY", ""),
        os.environ.get("GITHUB_TOKEN", ""),
    )

    # Identify previous valid git tag (skipping any tag containing '${')
    all_tags = run_cmd(["git", "tag", "--sort=-creatordate"]).splitlines()
    valid_prev_tags = [t.strip() for t in all_tags if t.strip() and t.strip() != tag_name and "${" not in t]
    prev_tag = valid_prev_tags[0] if valid_prev_tags else ""

    if prev_tag:
        compare_subtitle = f"So sánh từ bản **{prev_tag}** ➔ **{tag_name}**"
        raw_commits = run_cmd(
            ["git", "log", f"{prev_tag}..HEAD", "--no-merges", "--pretty=format:- `%h` %s *(%cr)*"]
        ).splitlines()
        diff_stat_lines = run_cmd(["git", "diff", "--stat", prev_tag, "HEAD"]).splitlines()
        diff_stat = diff_stat_lines[-1].strip() if diff_stat_lines else ""
        raw_files = [
            line.strip()
            for line in run_cmd(["git", "diff", "--name-only", prev_tag, "HEAD"]).splitlines()
            if line.strip()
        ]
    else:
        compare_subtitle = f"Thống kê thay đổi trong bản **{tag_name}**"
        raw_commits = run_cmd(
            ["git", "log", "-n", "8", "HEAD", "--no-merges", "--pretty=format:- `%h` %s *(%cr)*"]
        ).splitlines()
        diff_stat_lines = run_cmd(["git", "show", "--stat", "--oneline", "HEAD"]).splitlines()
        diff_stat = diff_stat_lines[-1].strip() if len(diff_stat_lines) > 1 else ""
        raw_files = [
            line.strip()
            for line in run_cmd(["git", "show", "--pretty=", "--name-only", "HEAD"]).splitlines()
            if line.strip()
        ]

    changed_files = [f"- `{f}`" for f in raw_files[:20]]
    commits = [c for c in raw_commits if "Initial commit" not in c]
    summary_text = generate_ai_description(
        app_ver=app_ver,
        prev_tag=prev_tag,
        raw_files=raw_files,
        commits=commits,
    )

    lines = [
        f"## Bản phát hành CloudPlay **v{app_ver}** (`{tag_name}`)",
        f"> **Ngày tạo:** {build_date}  |  **Dung lượng APK:** `{apk_size_str}`  |  **Version Code:** `#{app_code}`  |  **Commit:** `{latest_hash}`",
        "",
        "### 🤖 Mô tả bản cập nhật bằng AI (AI Release Summary)",
        summary_text,
        "",
    ]

    if commits:
        lines.extend(
            [
                "### Các commit trong bản này",
                "\n".join(commits[:8]),
                "",
            ]
        )

    if diff_stat or changed_files:
        lines.append(f"<details><summary><b>Danh sách file thay đổi ({compare_subtitle})</b></summary>")
        lines.append("")
        if diff_stat:
            lines.append(f"**Thống kê:** `{diff_stat}`")
            lines.append("")
        if changed_files:
            lines.extend(changed_files)
        lines.append("</details>")
        lines.append("")

    repo_slug = os.environ.get("GITHUB_REPOSITORY", "").strip() or "muitenvn/CloudPlay-App"
    direct_gh_url = f"https://github.com/{repo_slug}/releases/download/{tag_name}/{apk_ver_name}"
    cdn_fast_1 = f"https://ghfast.top/{direct_gh_url}"
    cdn_fast_2 = f"https://gh-proxy.com/{direct_gh_url}"

    lines.extend(
        [
            "---",
            "### Tải về và cài đặt",
            f"- **[Tải file APK trực tiếp từ GitHub (`{apk_ver_name}`)]({direct_gh_url})** *(hoặc trong mục **Assets** bên dưới)*",
            f"- **[Link tải dự phòng 1 (qua ghfast.top)]({cdn_fast_1})**",
            f"- **[Link tải dự phòng 2 (qua gh-proxy.com)]({cdn_fast_2})**",
            "- **Cách cập nhật:** Tải file APK về và cài đè lên phiên bản đang có trên máy (không cần gỡ bản cũ).",
            f"- **Mã kiểm tra SHA-256:** `{apk_sha256}`",
            "",
        ]
    )

    release_notes = "\n".join(lines)
    with open("release-output/RELEASE_NOTES.md", "w", encoding="utf-8") as f:
        f.write(release_notes)

    print(release_notes)


if __name__ == "__main__":
    main()
