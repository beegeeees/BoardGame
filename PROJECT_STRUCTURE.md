# Project Structure

기준 경로: `C:\Users\yonso\AndroidStudioProjects\title`

## Top Level

```text
title/
├─ app/
├─ gradle/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradlew
├─ gradlew.bat
├─ local.properties
├─ SERVER_SOCKET_NOTE.md
├─ SCORE_INTEGRATION_NOTE.md
└─ PROJECT_STRUCTURE.md
```

## App Module

```text
app/
├─ build.gradle.kts
└─ src/
   ├─ main/
   │  ├─ AndroidManifest.xml
   │  ├─ java/com/example/adchaosdemo/
   │  │  ├─ BoardActivity.kt
   │  │  ├─ DemoRoom.kt
   │  │  ├─ LobbyActivity.kt
   │  │  ├─ LobbyListActivity.kt
   │  │  ├─ MiniGame3Activity.kt
   │  │  ├─ MiniGame3IntroActivity.kt
   │  │  ├─ NicknameActivity.kt
   │  │  ├─ OptionsActivity.kt
   │  │  ├─ RoomListAdapter.kt
   │  │  ├─ RoomLocalStore.kt
   │  │  ├─ ServerRoomGateway.kt
   │  │  ├─ SessionPrefs.kt
   │  │  ├─ SplashActivity.kt
   │  │  ├─ TitleActivity.kt
   │  │  ├─ VolumeMazeView.kt
   │  │  ├─ socket/
   │  │  │  ├─ BoardGameSocketClient.java
   │  │  │  ├─ SocketRoomController.java
   │  │  │  └─ protocol/
   │  │  │     ├─ ConnectionState.java
   │  │  │     ├─ GameSnapshot.java
   │  │  │     ├─ MessageTypes.java
   │  │  │     ├─ PlayerSnapshot.java
   │  │  │     ├─ RoomSnapshot.java
   │  │  │     ├─ SnapshotMessageMapper.java
   │  │  │     ├─ SocketEventListener.java
   │  │  │     └─ SocketMessage.java
   │  │  └─ score/
   │  │     ├─ BoardScoreController.kt
   │  │     ├─ ScoreEngine.kt
   │  │     ├─ ScoreEngineUsageExample.kt
   │  │     └─ ScoreModels.kt
   │  └─ res/
   │     ├─ drawable/
   │     ├─ layout/
   │     │  ├─ activity_board.xml
   │     │  ├─ activity_lobby.xml
   │     │  ├─ activity_lobby_list.xml
   │     │  ├─ activity_minigame3.xml
   │     │  ├─ activity_minigame3_intro.xml
   │     │  ├─ activity_nickname.xml
   │     │  ├─ activity_options.xml
   │     │  ├─ activity_splash.xml
   │     │  ├─ activity_title.xml
   │     │  ├─ dialog_rules_pager.xml
   │     │  └─ item_room.xml
   │     ├─ mipmap-*/
   │     ├─ values/
   │     │  ├─ colors.xml
   │     │  ├─ strings.xml
   │     │  └─ themes.xml
   │     └─ xml/
   │        ├─ backup_rules.xml
   │        └─ data_extraction_rules.xml
   ├─ test/
   │  └─ java/com/example/adchaosdemo/score/
   │     ├─ BoardScoreControllerTest.kt
   │     └─ ScoreEngineTest.kt
   └─ androidTest/
      └─ java/org/androidtown/title/
         └─ ExampleInstrumentedTest.kt
```

## Key Areas

- `socket/`: BoardGame 서버와 통신하는 WebSocket 클라이언트/프로토콜 계층
- `score/`: UI와 분리된 점수 로직 계층(팀원 보드판에 연동 대상)
- `RoomLocalStore.kt`: 디버그 모드용 로컬 임시 방 저장소
- `ServerRoomGateway.kt`: 앱 화면과 소켓 컨트롤러를 연결하는 게이트웨이

