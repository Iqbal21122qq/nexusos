# NexusOS Agent - Autonomous Android AI Assistant

NexusOS Agent is an autonomous Android agent powered by Google Gemini, screen perception (Accessibility UI Hierarchy Tree + WebP screenshots), root automation (libsu), continuous voice control (Bengali/English STT & TTS with Barge-In), local RAG memory, and human-in-the-loop safety guardrails.

## Features
- **Module 1: Screen Perception & UI Tree Parser**: Accessibility Service dumps a compact, token-optimized XML tree with exact coordinate mapping.
- **Module 2: Root Shell Management & Natural Touch Simulation**: Executes shell commands and simulates human-like jitter taps, swipes, and typing via `libsu`.
- **Module 3: Continuous Voice Agent**: Background voice service supporting English and Bengali (`bn-BD`) speech recognition with TTS and VAD barge-in.
- **Module 4: Local RAG & Memory**: SQLite database storing user skills, portfolio, proposal styles, and lead filters.
- **Module 5: Freelance Auto-Pilot**: Autonomous bidding and proposal engine for Upwork, Fiverr, and job marketplaces.
- **Module 6: Social CRM Engine**: Autonomous Facebook group lead hunter and page inbox responder.
- **Module 7: Safety Firewall & Hardware Kill-Switch**: Command firewall blocks destructive shell commands; pressing Volume Down 3 times immediately terminates automation.

---

## 🚀 How to Push this Project to GitHub (গিটহাবে পুশ করার নিয়ম)

এই প্রোজেক্টটি সরাসরি আপনার GitHub রিপোজিটরিতে আপলোড করার জন্য নিচের কমান্ডগুলো আপনার টার্মিনালে চালান:

### ১. গিটহাব রিপোজিটরি তৈরি করুন
GitHub (https://github.com) এ গিয়ে একটি নতুন রিপোজিটরি তৈরি করুন (যেমন: `NexusOSAgent`)।

### ২. লোকাল কোড কমিট এবং পুশ করুন
```bash
# গিট রিপোজিটরি ইনিশিয়ালাইজ করুন
git init

# সব ফাইল যোগ করুন
git add .

# কমিট মেসেজ দিন
git commit -m "Initial commit: NexusOS Agent autonomous system"

# ব্রাঞ্চের নাম main করুন
git branch -M main

# আপনার GitHub রিপোজিটরি লিংক যোগ করুন (YOUR_USERNAME এবং REPO_NAME পরিবর্তন করুন)
git remote add origin https://github.com/YOUR_USERNAME/NexusOSAgent.git

# কোড গিটহাবে পুশ করুন
git push -u origin main
```

---

## ⚙️ GitHub Actions দিয়ে অটোমেটিক APK তৈরি (.yml Workflow)

এই প্রোজেক্টে `.github/workflows/android.yml` ফাইলটি যুক্ত করা আছে।
আপনি কোড গিটহাবে পুশ করলেই GitHub Actions স্বয়ংক্রিয়ভাবে:
1. কোড চেকআউট করবে।
2. Java 17 ও Gradle সেটআপ করবে।
3. সম্পূর্ণ Android APK বিল্ড করবে।
4. **Actions** ট্যাব থেকে আপনি সরাসরি `nexus-os-agent-debug-apk` ডাউনলোড করে আপনার ফোনে ইনস্টল করতে পারবেন!

---

## 🔑 Gemini API Key Configuration
1. অ্যাপের ভেতরে **Settings Screen** এ গিয়ে আপনার **Google AI Studio API Key** পেস্ট করুন এবং মডেল সিলেক্ট করুন (যেমন: `gemini-2.5-flash`)।
2. অথবা AI Studio Secrets Panel / `.env` ফাইলে `GEMINI_API_KEY=YOUR_KEY` সেট করুন।
