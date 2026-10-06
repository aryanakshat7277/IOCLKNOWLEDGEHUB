# IOCL Knowledge Hub

A premium Android application designed as a centralized knowledge-sharing platform. This app serves as a hub for accessing company magazines, live news, administrative notices, and an AI-powered Chatbot. 

## Features
- **God-Level Dashboard**: An ultra-premium, animation-rich dashboard featuring a parallax mesh gradient header, glassmorphism stat rings, and dynamic quick actions.
- **Magazines & Newspapers**: Read, download, and bookmark internal publications and notices. Includes native PDF viewing capabilities.
- **AI Chatbot Assistant**: Integrated AI capabilities to assist with queries (powered by Gemini, Groq, DeepSeek, OpenAI, or OpenRouter).
- **Live News**: Stay up-to-date with the latest organizational news and announcements.
- **Role-Based Access**: Specialized administrative panel for authorized users to manage accounts, upload documents, and view engagement analytics.

## Tech Stack
- **Architecture**: Clean Architecture + MVVM
- **UI Toolkit**: Jetpack Compose
- **Networking**: Retrofit & OkHttp
- **Dependency Injection**: Dagger Hilt
- **Database**: Room (Local caching) & Firebase Firestore
- **Image Loading**: Coil
- **AI Integration**: Google Generative AI SDK & Custom REST APIs

## Setup & Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/IOCL-Knowledge-Hub.git
   ```

2. **Configure API Keys**:
   - Create a `.env` file in the root directory.
   - Add your API keys for the AI services as follows:
     ```env
     GEMINI_API_KEY=your_gemini_key_here
     GROQ_API_KEY=your_groq_key_here
     DEEPSEEK_API_KEY=your_deepseek_key_here
     OPENAI_API_KEY=your_openai_key_here
     OPENROUTER_API_KEY=your_openrouter_key_here
     ```
     *(Note: The `.env` file is intentionally ignored by Git to keep your keys safe).*

3. **Open in Android Studio**:
   - Launch Android Studio.
   - Select **File > Open** and choose the `IOCL-Knowledge-Hub` directory.
   - Wait for the initial Gradle sync to complete.

4. **Run the App**:
   - Connect an Android device or start an emulator.
   - Click the Run button (`Shift + F10`).
