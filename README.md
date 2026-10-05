# busan-city-roulette
# 🎲 City Roulette

부산의 대중교통과 공공데이터를 활용하여
사용자에게 즉흥적인 여행 목적지와 코스를 추천하는 웹 서비스입니다.

## 📌 Project Overview

기존 여행 서비스는 사용자가 목적지를 먼저 선택하고
이동 경로나 주변 장소를 검색하는 방식입니다.

City Roulette는 반대로 사용자의 현재 위치와 이용 가능한 시간 등을 기반으로
갈 수 있는 장소를 탐색하고 목적지를 추천합니다.

사용자는 여행지를 직접 정하지 않아도
부산 곳곳을 새로운 방식으로 탐험할 수 있습니다.

## 🎯 Problem

여행이나 외출을 하고 싶어도

- 어디를 갈지 결정하기 어렵고
- 항상 비슷한 장소만 방문하게 되며
- 이동 시간이나 날씨를 일일이 확인해야 하는

문제가 있습니다.

City Roulette는 부산의 공공데이터를 활용하여
이러한 의사결정 과정을 자동화하는 것을 목표로 합니다.

## ✨ Key Features

### 1. 🎲 Random Destination

사용자의 출발지와 이용 가능한 시간을 기반으로
방문 가능한 목적지를 랜덤하게 추천합니다.

### 2. 🚌 Public Transportation Based Recommendation

부산 버스 및 대중교통 정보를 활용하여
실제로 이동 가능한 장소를 후보로 선정합니다.

### 3. 🌤 Context-aware Recommendation

날씨, 관광지 정보 등을 활용하여
현재 상황에 적합한 목적지와 여행 코스를 제공합니다.

## 🗂 Data / API

사용 예정 데이터

- 부산광역시 버스정보 API
- 한국관광공사 관광정보 API
- 기상청 날씨 API
- 공공데이터포털 Open API

향후 프로젝트 진행 과정에 따라 변경될 수 있습니다.

## 🛠 Tech Stack

### Backend

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA

### Frontend

- Thymeleaf
- HTML
- CSS
- JavaScript

### Database

- H2
- MySQL

### External API

- 공공데이터포털 Open API

### Version Control

- Git
- GitHub

## 🗓 Milestones

### Week 1-4
- 프로젝트 기획
- 개발 환경 구축
- API 조사
- UI 및 데이터 구조 설계

### Week 5-8
- 공공데이터 API 연동
- 목적지 데이터 조회
- 랜덤 목적지 추천 MVP 구현

### Week 9-12
- 사용자 조건 기반 추천
- 날씨 및 교통 데이터 연동
- 여행 기록 기능 구현

### Week 13-16
- UI 개선
- 예외 처리
- 테스트
- 문서화
- 최종 발표 준비

## 📄 License

MIT License
