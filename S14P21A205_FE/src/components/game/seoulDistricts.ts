// Simplified Seoul district boundaries (normalized coordinates centered at 0,0)
// Each district is a polygon outline approximating the real shape

export interface DistrictGeo {
  id: number;
  name: string;
  grade: string;
  rent: string;
  congestion: string;
  tags: string[];
  description: string;
  // Simplified polygon points [x, z] normalized roughly to [-5, 5] range
  polygon: [number, number][];
  // Label position
  center: [number, number];
}

export const seoulDistricts: DistrictGeo[] = [
  {
    id: 1, name: "홍대", grade: "A등급", rent: "₩400,000", congestion: "High",
    tags: ["Youth", "Music", "Art"],
    description: "젊음과 문화의 거리. 인디 음악과 스트릿 아트가 공존하는 MZ세대의 핫플레이스.",
    polygon: [[-3.8, -1.2], [-2.8, -1.6], [-2.4, -0.8], [-2.6, -0.2], [-3.4, -0.1], [-3.9, -0.6]],
    center: [-3.1, -0.7],
  },
  {
    id: 2, name: "여의도", grade: "B등급", rent: "₩350,000", congestion: "Medium",
    tags: ["Finance", "Office"],
    description: "금융의 중심지. 직장인 대상 런치 팝업이 유리합니다.",
    polygon: [[-3.6, 1.0], [-2.8, 0.6], [-2.2, 1.0], [-2.4, 1.8], [-3.0, 2.0], [-3.7, 1.6]],
    center: [-3.0, 1.3],
  },
  {
    id: 3, name: "명동", grade: "S등급", rent: "₩500,000", congestion: "Very High",
    tags: ["Tourist", "Shopping"],
    description: "관광객의 성지. 외국인 방문객이 가장 많은 쇼핑 거리.",
    polygon: [[-1.2, -0.8], [-0.4, -1.2], [0.2, -0.6], [0.0, 0.0], [-0.6, 0.2], [-1.4, -0.2]],
    center: [-0.6, -0.4],
  },
  {
    id: 4, name: "이태원", grade: "B등급", rent: "₩300,000", congestion: "Medium",
    tags: ["Global", "Food"],
    description: "다국적 문화가 공존하는 거리. 이색적인 팝업에 적합.",
    polygon: [[-0.6, 0.2], [0.2, -0.2], [0.8, 0.4], [0.6, 1.2], [-0.2, 1.4], [-0.8, 0.8]],
    center: [0.0, 0.6],
  },
  {
    id: 5, name: "성수", grade: "S등급", rent: "₩300,000", congestion: "High",
    tags: ["Hip_Vibe", "Cafe_Tour", "Fashion"],
    description: "MZ세대의 놀이터이자 팝업스토어 성지. 트렌디한 카페와 편집숍이 즐비한 핫플레이스.",
    polygon: [[1.4, -1.4], [2.6, -1.6], [3.0, -0.8], [2.8, -0.1], [2.0, 0.0], [1.2, -0.6]],
    center: [2.2, -0.7],
  },
  {
    id: 6, name: "건대", grade: "B등급", rent: "₩250,000", congestion: "Medium",
    tags: ["University", "Nightlife"],
    description: "대학가 특유의 활기. 저렴한 임대료 대비 높은 유동인구.",
    polygon: [[2.8, -0.1], [3.8, -0.4], [4.2, 0.4], [3.8, 1.0], [3.0, 1.0], [2.6, 0.4]],
    center: [3.4, 0.4],
  },
  {
    id: 7, name: "강남", grade: "S등급", rent: "₩600,000", congestion: "Very High",
    tags: ["Premium", "Business"],
    description: "대한민국 최고의 상권. 높은 임대료만큼 높은 수익 잠재력.",
    polygon: [[0.2, 1.6], [1.4, 1.2], [2.2, 1.6], [2.0, 2.6], [1.2, 3.0], [0.4, 2.6]],
    center: [1.2, 2.1],
  },
  {
    id: 8, name: "잠실", grade: "A등급", rent: "₩350,000", congestion: "High",
    tags: ["Family", "Entertainment"],
    description: "롯데월드와 석촌호수. 가족 단위 방문객이 많은 지역.",
    polygon: [[2.6, 1.2], [3.6, 0.8], [4.4, 1.4], [4.2, 2.2], [3.4, 2.6], [2.6, 2.0]],
    center: [3.5, 1.7],
  },
];
