# BassShot (bass-shot) 🎣📏

> **AI 기반 배스 자동 계측 및 출조 기록 카메라 안드로이드 앱**

배스 낚시인을 위한 프로페셔널 아웃도어 카메라 앱입니다. 배스의 아랫입술을 손(장갑)으로 잡고 세로로 들어 촬영하면 실시간 출조 환경 정보(시간, 날짜, 날씨/기온/풍속/기압, 한글 주소, 미니 정적 지도)를 프로스티드 글래스 HUD 스타일로 오버레이하고, 촬영 직후 **Gemini Flash Vision AI**를 통해 배스의 전장(cm), 체고(cm), 예측 무게(g)를 자동 계측하여 `DCIM/BassShot` 폴더에 고화질 JPG로 저장합니다.

---

## ✨ 핵심 기능

### 1. 실시간 출조 환경 HUD 오버레이 (`CAMERA_PREVIEW`)
- **실시간 시간 및 날짜**: 1초 단위 실시간 시계(`HH:mm`)와 나노 바나나 스타일 깔끔한 한국어 날짜/요일(`2026. 10. 24 (토)`)
- **날씨 및 환경 정보**: 날씨(아이콘), 기온(°C), 풍속(m/s) 표시
- **배스 낚시 기준 기압 인디케이터**:
  - **저기압 (`<= 1010 hPa`)**: **일렉트릭 사이언 (`#00E5FF`)** — 수압 완화로 배스 부상 & 피딩 찬스 상태
  - **평균 기압 (`1011 ~ 1014 hPa`)**: **옵티컬 화이트 (`#FFFFFF`)** — 표준 대기압(1013hPa) 부근의 안정적 상태
  - **고기압 (`>= 1015 hPa`)**: **골든 앰버 (`#FBBF24`)** — 맑은 햇살, 깊은 수심 및 커버 그늘 공략 필요
  - **정수형 표기**: 기상청 및 낚시 기상 앱 표준에 맞춰 소수점을 생략(`기압 1001hPa`)하여 말줄임표 잘림 방지 및 시인성 극대화
  - 기기 내장 기압계(`Sensor.TYPE_PRESSURE`) 최우선 활용 (센서 부재 시 OpenWeatherMap 해면기압 폴백)
- **위치 및 지도**: `Geocoder`를 통한 한국어 지번/도로명 주소 변환 및 Google Static Maps API 연동 정사각형 미니 정적 지도
- **슬림 컴팩트 디자인**:
  - 카드 가로 폭 `120dp`, 세로 높이 `74dp`로 중앙 사진 시야 개방감 40% 확장
  - 카드 배경 테두리를 제거하고 모서리를 완전 밀착하여 피사체를 가리지 않는 미니멀 룩 완성
  - 단말기 시스템 글꼴 크기 설정에 영향받지 않도록 폰트 스케일 1.0f 원천 고정

### 2. 일반 촬영 / AI 계측 듀얼 모드 지원
- **일반 촬영 모드**:
  - 셔터를 누르면 출조 환경 정보가 합성된 고화질 사진이 백그라운드로 즉시 자동 저장
  - 화면 정지 없이 카메라 프리뷰가 유지되어 빠르고 쾌적한 연속 촬영 지원
- **AI 계측 모드**:
  - Gemini Flash 모델(`gemini-3.5-flash-lite` / `gemini-3.5-flash` 자동 폴백)을 통해 주먹(장갑) 기준 폭(기본 9.5cm) 대비 픽셀 비율 측정
  - 어류학 체장-체고-무게 공식 적용: $W = 0.045 \times L \times G^2$
  - 배스 미검출 또는 통신 단절 시 즉시 수동 직접 계측 모드로 원활하게 전환
- **모드 전환**: 화면 하단 캡슐형 탭으로 터치 한 번에 모드 전환 (마지막 선택 모드 자동 유지)

### 3. 배스 체장 티어별 길이 색상 차등화
- 우상단 오버레이의 배스 길이(체장) 색상이 배스 낚시 문화와 게이밍 티어 감성을 살린 4단계 위계 컬러로 자동 전환:
  - **1/2짜 (짜치, `< 30.0cm`)**: **티타늄 실버 (`#E2E4E9`)** — 깨끗하고 단정한 실버
  - **3짜 (준척급, `30.0 ~ 39.9cm`)**: **네온 라임 (`#A3E635`)** — 파릇파릇한 시그니처 형광 라임
  - **4짜 (대형급, `40.0 ~ 49.9cm`)**: **일렉트릭 사이언 (`#00E5FF`)** — 묵직한 손맛의 사이언 블루
  - **5짜 이상 (런커, `>= 50.0cm`)**: **런커 골드 (`#FFB703`)** — 찬란하게 빛나는 트로피 골드
- 카메라 프리뷰, 계측 결과 화면, 저장되는 고해상도 합성 사진에 1:1 완벽 반영

### 4. 인터랙티브 캘리퍼스 미세조정 & 고화질 원본 합성 (`RESULT_SAVED`)
- **실시간 드래그 미세조정**: 주둥이, 꼬리, 체고 좌/우 마커를 직접 드래그하여 길이와 무게 실시간 재계산
- **고해상도 Canvas 직접 합성**: 화면 캡처가 아닌 **원본 고해상도 Canvas 직접 합성(`HighResCanvasCompositor`)**으로 텍스트 및 가이드라인의 벡터 수준 선명도 유지
- **풍부한 EXIF 메타데이터 보존**: 사진 EXIF JSON 필드에 계측 좌표, 전장/체고/무게 및 출조지 환경 정보 완전 보존
- **원본 백업 지원**: 설정에서 **'무보정 원본 함께 저장'** 활성화 시 `_ORIG.jpg` 원본 동시 저장

### 5. 사진 뷰어 갤러리 및 원클릭 GitHub 이슈 제보
- **인앱 사진 뷰어(`PhotoViewerDialog`)**: 저장된 과거 출조 사진을 스와이프하며 조회하고, 카카오톡 전송, 시스템 공유, 삭제 및 미계측 사진 재계측 지원
- **원클릭 오류 제보 시스템**:
  - 결과 화면 및 사진 뷰어 우상단 오류 제보 버튼(`[🐛]`, SafetyOrange) 제공
  - 촬영 사진, AI 계측 메타데이터, 환경 정보, 앱 로컬 로그를 원클릭으로 GitHub 이슈로 자동 생성 및 첨부

### 6. 무음 촬영 및 자동 업데이트
- **무음 촬영 기본화**: 조용한 필드 환경에서 방해 없이 촬영 가능
- **인앱 자동 업데이트**: 앱 실행 시 GitHub Releases 최신 버전을 확인하여 원클릭 인앱 업데이트 제공
- **GitHub Actions CI/CD**: `v*` 태그 푸시 시 릴리즈 APK 빌드 및 GitHub Releases 자동 배포

---

## 🛠️ 기술 스택

- **Language & UI:** Kotlin, Jetpack Compose (Material3, Tactical Dark Theme, Nano Banana HUD)
- **Architecture:** MVVM (ViewModel, StateFlow, Repository Pattern)
- **Camera:** AndroidX CameraX 1.4.0 (`LifecycleCameraController`)
- **AI Vision:** Google AI Client SDK for Android (`gemini-3.5-flash-lite` / `gemini-3.5-flash`)
- **Location & Sensors:** Google Play Services Location, Android `SensorManager` (`Sensor.TYPE_PRESSURE`)
- **Network & Storage:** Retrofit2 + Kotlinx Serialization, OkHttp3, AndroidX DataStore Preferences, MediaStore API, AndroidX ExifInterface
- **CI/CD:** GitHub Actions (Release APK Build & Auto GitHub Release)

---

## 🔑 API 키 설정 (`local.properties`)

프로젝트 루트의 `local.properties` 파일에 API 키를 입력합니다:

```properties
sdk.dir=/Users/.../Library/Android/sdk
GEMINI_API_KEY=your_gemini_api_key_here
WEATHER_API_KEY=your_openweathermap_api_key_here
MAPS_API_KEY=your_google_maps_static_api_key_here
GH_ISSUE_TOKEN=your_github_personal_access_token_here
```

---

## 🚀 빌드 및 테스트

```bash
# 단위 테스트 실행 (체장 티어, 기압 판정, 계측 공식 검증)
./gradlew testDebugUnitTest

# 디버그 APK 빌드
./gradlew assembleDebug
# 산출물: app/build/outputs/apk/debug/app-debug.apk

# 릴리즈 APK 빌드
./gradlew assembleRelease
# 산출물: app/build/outputs/apk/release/app-release.apk
```
