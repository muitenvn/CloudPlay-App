#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generates a concise APK release description and packages the APK for GitHub Releases.
- Extracts exact resolved versionName & versionCode from Gradle's output-metadata.json
- Calls lightweight AI (Gemini 3.1 Flash-Lite or GitHub Models via built-in GITHUB_TOKEN)
  with <200 output tokens to write a short 3-5 bullet update description
- Automatically cleans up any legacy malformed tags containing '${runNumber}'
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


def extract_changelog_bullets() -> list[str]:
    """Extracts concise bullet points from the latest version in CHANGELOG.md."""
    if not os.path.isfile("CHANGELOG.md"):
        return []
    bullets: list[str] = []
    in_latest = False
    try:
        with open("CHANGELOG.md", "r", encoding="utf-8") as f:
            for raw_line in f:
                line = raw_line.strip()
                if line.startswith("## ["):
                    if in_latest:
                        break
                    in_latest = True
                    continue
                if in_latest:
                    if line.startswith("---"):
                        break
                    if line.startswith("- **"):
                        m = re.match(r"^-\s*\*\*([^*]+)\*\*:?\s*([^.;]+)", line)
                        if m:
                            title = m.group(1).strip().rstrip(":")
                            desc = m.group(2).strip()
                            # Remove long parenthesized class lists to keep summary clean for users
                            desc = re.sub(r"\s*\(`[^)]+\)", "", desc).strip()
                            bullets.append(f"- **{title}:** {desc}.")
                        else:
                            bullets.append(line)
    except Exception:
        pass
    return bullets[:5]


def generate_ai_description(
    app_ver: str,
    bullets: list[str],
    commits: list[str],
    changed_files: list[str],
) -> str:
    """Calls Gemini 3.1 Flash-Lite or GitHub Models with minimal tokens, or falls back to concise bullets."""
    context_parts = []
    if commits:
        context_parts.append("Commits:\n" + "\n".join(commits[:6]))
    if changed_files:
        context_parts.append("Files:\n" + "\n".join(changed_files[:10]))
    if bullets:
        context_parts.append("Highlights:\n" + "\n".join(bullets[:5]))

    compact_context = "\n\n".join(context_parts)[:900]
    prompt = (
        f"Viết mô tả ngắn gọn (3-5 gạch đầu dòng bằng Tiếng Việt, súc tích, dễ hiểu cho người chơi) "
        f"tóm tắt những điểm mới trong bản cập nhật APK CloudPlay Xbox v{app_ver} dựa trên:\n"
        f"{compact_context}"
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

    # 1. Try Gemini 3.1 Flash-Lite (ultra-low quota / free tier)
    if gemini_key and gemini_key != "MY_GEMINI_API_KEY":
        try:
            url = (
                "https://generativelanguage.googleapis.com/v1beta/models/"
                f"gemini-3.1-flash-lite-preview:generateContent?key={gemini_key}"
            )
            payload = {
                "contents": [{"parts": [{"text": prompt}]}],
                "generationConfig": {
                    "temperature": 0.3,
                    "maxOutputTokens": 220,
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
                if text:
                    return text
        except Exception:
            pass

    # 2. Try GitHub Models API (gpt-4o-mini) via built-in GITHUB_TOKEN (with models: read permission)
    if gh_token:
        for endpoint, model_id in (
            ("https://models.github.ai/inference/chat/completions", "openai/gpt-4o-mini"),
            ("https://models.inference.ai.azure.com/chat/completions", "gpt-4o-mini"),
        ):
            try:
                payload = {
                    "model": model_id,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 220,
                }
                req = urllib.request.Request(
                    endpoint,
                    data=json.dumps(payload).encode("utf-8"),
                    headers={
                        "Content-Type": "application/json",
                        "Authorization": f"Bearer {gh_token}",
                    },
                    method="POST",
                )
                with urllib.request.urlopen(req, timeout=15) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    text = data["choices"][0]["message"]["content"].strip()
                    if text:
                        return text
            except Exception:
                pass

    # 3. Clean concise fallback (4 short bullets, never a giant wall of text)
    if bullets:
        return "\n".join(bullets[:4])
    if commits:
        return "\n".join(commits[:4])
    return f"- Cập nhật hiệu năng, độ ổn định luồng stream và tối ưu kết nối cho phiên bản v{app_ver}."


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
        changed_files = [
            f"- `{line.strip()}`"
            for line in run_cmd(["git", "diff", "--name-only", prev_tag, "HEAD"]).splitlines()
            if line.strip()
        ][:20]
    else:
        compare_subtitle = f"Thống kê thay đổi trong bản **{tag_name}**"
        raw_commits = run_cmd(
            ["git", "log", "-n", "8", "HEAD", "--no-merges", "--pretty=format:- `%h` %s *(%cr)*"]
        ).splitlines()
        diff_stat_lines = run_cmd(["git", "show", "--stat", "--oneline", "HEAD"]).splitlines()
        diff_stat = diff_stat_lines[-1].strip() if len(diff_stat_lines) > 1 else ""
        changed_files = [
            f"- `{line.strip()}`"
            for line in run_cmd(["git", "show", "--pretty=", "--name-only", "HEAD"]).splitlines()
            if line.strip()
        ][:20]

    commits = [c for c in raw_commits if "Initial commit" not in c]
    bullets = extract_changelog_bullets()
    summary_text = generate_ai_description(app_ver, bullets, commits, changed_files)

    lines = [
        f"## 🎮 Bản cập nhật CloudPlay Xbox **v{app_ver}** (`{tag_name}`)",
        f"> 📅 **Phát hành:** {build_date}  |  📦 **Kích thước APK:** `{apk_size_str}`  |  🔢 **Version Code:** `#{app_code}`  |  🔖 **Commit:** `{latest_hash}`",
        "",
        "### ✨ Mô tả bản cập nhật",
        summary_text,
        "",
    ]

    if commits:
        lines.extend(
            [
                "### 🕒 Lịch sử Commit",
                "\n".join(commits[:8]),
                "",
            ]
        )

    if diff_stat or changed_files:
        lines.append(f"<details><summary>📂 <b>Chi tiết các file đã thay đổi ({compare_subtitle})</b></summary>")
        lines.append("")
        if diff_stat:
            lines.append(f"**Tổng quan:** `{diff_stat}`")
            lines.append("")
        if changed_files:
            lines.extend(changed_files)
        lines.append("</details>")
        lines.append("")

    lines.extend(
        [
            "---",
            "### 📥 Tải về & Cài đặt đè trực tiếp",
            f"- **File APK bản cập nhật này:** Nhấn tải **`{apk_ver_name}`** tại mục **Assets** bên dưới.",
            "- **Cài đặt đè không mất dữ liệu:** APK được ký bằng chữ ký cố định — cài đặt đè trực tiếp lên bản cũ mà không cần gỡ ứng dụng.",
            f"- **SHA-256:** `{apk_sha256}`",
            "",
        ]
    )

    release_notes = "\n".join(lines)
    with open("release-output/RELEASE_NOTES.md", "w", encoding="utf-8") as f:
        f.write(release_notes)

    print(release_notes)


if __name__ == "__main__":
    main()
