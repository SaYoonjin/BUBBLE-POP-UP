# BUBBLE-POP-UP

실시간 팝업스토어 경영 시뮬레이션 프로젝트입니다.

사용자는 시즌에 참가해 점포 입지와 메뉴를 선택하고, 발주와 액션을 활용해 하루 단위로 매출을 운영합니다. 저장소는 프론트엔드, 백엔드, 데이터 파이프라인 및 운영 설정을 함께 관리하는 모노레포 구조입니다.

## Overview

- OAuth2 + JWT 기반 인증
- 시즌 참가, 영업 진행, 일일 리포트, 최종 랭킹 제공
- MySQL + Redis 기반 실시간 게임 상태 및 랭킹 관리
- Spark + HDFS 기반 환경 데이터 ETL
- Prometheus + Grafana 기반 운영 모니터링

## Repository Structure

- [S14P21A205_FE](S14P21A205_FE/README.md): Vite 기반 프론트엔드
- [S14P21A205_BE](S14P21A205_BE/README.md): Spring Boot 백엔드
- `S14P21A205_BE/monitoring`: Prometheus, Grafana 설정
- `S14P21A205_BE/ops`: 배포 및 운영 스크립트

## Tech Stack

- Frontend: React, Vite, TypeScript, Tailwind CSS
- Backend: Java 17, Spring Boot, Spring Security, JPA, Flyway
- Database/Cache: MySQL, Redis
- Data: Spark, HDFS
- Infra: Docker Compose, Nginx
- Monitoring: Prometheus, Grafana

## Key Features

- 실시간 시즌 진행과 점포 운영 API
- 실시간 랭킹 계산 및 Redis 캐시
- 발주, 할인, 기부, 긴급발주 등 액션 시스템
- 시즌 종료 후 최종 랭킹 및 보상 처리
- 외부 데이터 기반 환경 점수 ETL

## Quick Links

- Frontend README: [S14P21A205_FE/README.md](S14P21A205_FE/README.md)
- Backend README: [S14P21A205_BE/README.md](S14P21A205_BE/README.md)

