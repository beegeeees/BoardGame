# Socket Dev Note

- `title` 앱은 WebSocket 클라이언트로 동작합니다.
- 기본 서버 주소: `ws://10.0.2.2:8080/game`
- 테스트 중 Firebase 연동이 없으면 클라이언트는 `firebaseIdToken="DEV_TOKEN"`을 전송합니다.
- 서버는 개발 모드로 아래 옵션을 켜서 실행하세요.
  - `BOARDGAME_DEV_AUTH=true`

## 실행 구조

- 서버: `C:\Users\yonso\StudioProjects\BoardGame\socket-server` (별도 JVM 프로세스)
- 앱: `C:\Users\yonso\AndroidStudioProjects\title` (안드로이드 클라이언트)

