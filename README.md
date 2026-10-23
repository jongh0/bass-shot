# BassShot (bass-shot) 🎣📏

> **AI 기반 배스 자동 계측 및 타임마크 카메라 안드로이드 앱**

배스 낚시인을 위한 프로페셔널 아웃도어 카메라 앱입니다. 배스의 아랫입술을 손(장갑)으로 잡고 세로로 들어 촬영하면 실시간 환경 정보(시간, 날짜, 날씨/기온/풍속/기압, 한글 주소, 미니 정적 지도)를 Timemark 스타일로 오버레이하고, 촬영 직후 **Gemini Flash Vision AI**를 통해 배스의 전장(cm), 체고(cm), 예측 무게(g)를 자동 계측하여 `DCIM/BassShot` 폴더에 고화질 JPG로 저장합니다.

---

## ✨ 핵심 기능

1. **실시간 Timemark 오버레이 (`CAMERA_PREVIEW`)**
   - 1초 단위 실시간 시계(`HH:mm`) + Leica Amber 수직 분할 바
   - 한국어 날짜/요일 (`2026년 10월 23일 금요일`)
   - 날씨, 기온(°C), 풍속(m/s), 기압(hPa) 통합 표시
   - 기기 내장 기압계(`Sensor.TYPE_PRESSURE`) 최우선 활용 (없을 시 OpenWeatherMap 해면기압 폴백)
   - `Geocoder`를 통한 한국어 지번/도로명 주소 변환
   - Google Static Maps API 연동 정사각형 미니 정적 지도
2. **즉시 방생 지원 AI 비전 계측 (`ANALYZING`)**
   - 셔터 클릭 즉시 정지 이미지를 화면에 고정하여 배스를 바로 물에 놓아줄 수 있도록 지원
   - Gemini Flash 모델(`gemini-2.5-flash` / `gemini-2.0-flash` 자동 폴백)을 통해 주먹(장갑) 기준 폭(기본 9.5cm) 대비 픽셀 비율 측정
   - 어류학 체장-체고 공식 적용: $W = 0.045 \times L \times G^2$
   - 배스 미검출 또는 통신 단절 시 즉시 수동 캘리퍼스 모드로 자동 전환
3. **인터랙티브 캘리퍼스 미세조정 & 고화질 저장 (`RESULT_SAVED`)**
   - 주둥이, 꼬리, 체고 좌/우 마커를 직접 드래그하여 길이와 무게 실시간 재계산
   - 화면 캡처가 아닌 **원본 고해상도 Canvas 직접 합성(`HighResCanvasCompositor`)**으로 텍스트 및 가이드라인의 벡터 수준 선명도 유지
   - `DCIM/BassShot` 앨범에 `BASS_yyyyMMdd_HHmmss.jpg`로 저장
   - 설정에서 **'무보정 원본 함께 저장'** 활성화 시 `_ORIG.jpg` 원본 동시 백업
4. **인앱 자동 업데이트 & GitHub Actions CI/CD**
   - 앱 실행 시 GitHub Releases 최신 버전을 조회하여 원클릭 인앱 업데이트 제공
   - `v*` 태그 푸시 시 Release APK 빌드 및 GitHub Release 자동 배포

---

## 🛠️ 기술 스택

- **Language & UI:** Kotlin, Jetpack Compose (Material3, Tactical Matte Black & Leica Amber Dark Theme)
- **Architecture:** MVVM (ViewModel, StateFlow, Repository Pattern)
- **Camera:** AndroidX CameraX 1.4.0 (`LifecycleCameraController`)
- **AI Vision:** Google AI Client SDK for Android (`gemini-2.5-flash`)
- **Location & Sensors:** Google Play Services Location, Android `SensorManager` (`Sensor.TYPE_PRESSURE`)
- **Network & Storage:** Retrofit2 + Kotlinx Serialization, OkHttp3, AndroidX DataStore Preferences, MediaStore API

---

## 🔑 API 키 설정 (`local.properties`)

프로젝트 루트의 `local.properties` 파일에 API 키를 입력합니다 (Git 추적 제외):

```properties
sdk.dir=/Users/.../Library/Android/sdk
GEMINI_API_KEY=your_gemini_api_key_here
WEATHER_API_KEY=your_openweathermap_api_key_here
MAPS_API_KEY=your_google_maps_static_api_key_here
```

---

## 🚀 빌드 및 테스트

```bash
# 단위 테스트 실행 (무게 공식 및 비례 계산 검증)
./gradlew testDebugUnitTest

# 디버그 APK 빌드
./gradlew assembleDebug
# 산출물: app/build/outputs/apk/debug/app-debug.apk
```
