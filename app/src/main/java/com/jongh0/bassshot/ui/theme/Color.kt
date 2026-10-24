package com.jongh0.bassshot.ui.theme

import androidx.compose.ui.graphics.Color

// Optical White & Titanium Monochrome Palette (Pure Camera Reticle Aesthetic)
val ObsidianBlack = Color(0xFF0B0C0E) // 딥 캔버스 블랙
val AnodizedCharcoal = Color(0xFF16171B) // 아노다이즈드 다크 차콜 (컨트롤 바, 다이얼로그)
val SmokeGlass = Color(0xCC16171B) // 80% 투명 스모크 글래스
val SubtleTitanium = Color(0xFF282A30) // 티타늄 헤어라인 보더
val HairlineBorder = Color(0x33FFFFFF) // 20% 투명 화이트 보더

// Crisp Precision Optical Accents (촌스러운 노란색/원색 완전 배제)
val OpticalWhite = Color(0xFFFFFFFF) // 크리스프 옵티컬 화이트 (주 캘리퍼스 선, 레티클 조준선, 셔터 링)
val OpticalWhiteGlow = Color(0x40FFFFFF) // 드래그 시 은은한 화이트 발광 아우라
val TitaniumSilver = Color(0xFFE2E4E9) // 세련된 티타늄 실버 (액션 버튼, 주요 수치)
val StudioBlue = Color(0xFF38BDF8) // 차분한 아이스 스튜디오 블루 포인트 (필요 시 미세 강조)
val ErrorRed = Color(0xFFEF4444) // 에러
val SafetyOrange = Color(0xFFFF9800) // 계측 오류 제보 포인트 오렌지
val GunmetalGrey = Color(0xFF32343A)

// Nano Banana Modern HUD Accents
val ElectricCyan = Color(0xFF00E5FF) // 일렉트릭 사이언 (캘리퍼 브래킷, 중앙 대시선, 저기압 포인트)
val ElectricCyanGlow = Color(0x6600E5FF) // 사이언 글로우 아우라
val NeonLime = Color(0xFFA3E635) // 네온 라임 ("길이", "35.2 cm", 3짜 포인트)
val SunAmber = Color(0xFFFBBF24) // 고기압 골든 앰버 포인트
val LunkerGold = Color(0xFFFFB703) // 5짜 이상 런커 트로피 골드 포인트
val FrostedGlassBg = Color(0x38000000) // 프로스티드 글래스 반투명 배경
val FrostedGlassBorder = Color(0x28FFFFFF) // 은은한 글래스 헤어라인 보더
val PillBadgeBg = Color(0xB3082F49) // 중앙 계측 캡슐 알약 배경

// Typography & Text (눈이 편안한 은은한 실버 & 웜 그래파이트)
val MutedSilver = Color(0xFFE2E4E9) // 메인 텍스트 (눈부시지 않은 정제된 실버)
val WarmGraphite = Color(0xFF8E9099) // 서브 텍스트 / 라벨
val DarkGrey = Color(0xFF32343A)

// Backwards compatibility aliases (노란색/골드 완전 제거 및 옵티컬 화이트/실버 매핑)
val ChampagneGold = OpticalWhite
val ChampagneGoldGlow = OpticalWhiteGlow
val LeicaAmber = OpticalWhite
val TacticalCyan = TitaniumSilver
val SoftSage = TitaniumSilver
val MatteBlack = ObsidianBlack
val DarkSurface = AnodizedCharcoal
val CoolGrey = WarmGraphite
