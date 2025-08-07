# Typerr

A feature-rich JavaFX typing speed test application with multiplayer capabilities, comprehensive statistics tracking, and customizable themes.

<div align="center">
    <img src="src/main/resources/images/icon.png" alt="Typerr Logo" width="25%">
</div>


## Features

### 🎮 Core Typing Test
- **Multiple Game Modes**: Time-based, word count, and custom challenges
- **Difficulty Levels**: Easy, medium, and hard word sets
- **Real-time Feedback**: Live WPM, accuracy, and error tracking
- **Progress Visualization**: Dynamic typing progress display

### 🌐 Multiplayer Gaming
- **Server/Client Architecture**: Host or join typing competitions
- **Real-time Synchronization**: Synchronized challenges across multiple players
- **Competitive Scoring**: Compare performance with other players
- **Round-based Gameplay**: Structured multiplayer sessions

### 📊 Statistics & Analytics
- **Comprehensive Tracking**: Detailed performance history
- **Visual Charts**: Progress visualization with interactive charts
- **Performance Metrics**: WPM, accuracy, consistency analysis
- **SQLite Database**: Persistent local statistics storage

### 🎨 Customization
- **Multiple Themes**: Light and dark theme support
- **Configurable Settings**: Customizable game parameters
- **Keyboard Shortcuts**: Comprehensive hotkey support
- **Responsive UI**: Adaptive interface design

### ⌨️ Advanced Features
- **Smart Word Provider**: Intelligent word selection algorithms
- **Error Analysis**: Detailed typing error tracking
- **Session Management**: Comprehensive test session handling
- **Resource Management**: Efficient font and theme loading

## About Documentations

**This project has been extensively documented using an LLM. Please checkout the WITH-DOC branch for files with detailed comments and documentation.**

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- JavaFX 17+


### First Launch
1. Launch the application
2. Choose your preferred theme (Light/Dark)
3. Select a game mode (Time-based, Word count, etc.)
4. Set difficulty level (Easy, Medium, Hard)
5. Start typing!

## Project Structure

```
Typerr/
├── src/main/java/com/typerr/
│   ├── Typerr.java                    # Main application entry point
│   ├── TestSession.java               # Core typing test logic
│   ├── database/                      # Database layer
│   ├── network/                       # Multiplayer networking
│   ├── ui/                            # User interface controllers
│   └── charts/                        # Statistics visualization
│   └── statics/                       # Constants and Enums
├── src/main/resources/             # Application resources
├── docs/                           # Documentation
└── pom.xml                         # Maven configuration
```


P.S. Typer Logo image taken from Google
