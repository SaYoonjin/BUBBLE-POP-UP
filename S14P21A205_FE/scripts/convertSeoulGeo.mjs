import { readFileSync, writeFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const raw = JSON.parse(readFileSync(resolve(__dirname, '../node_modules/.cache/seoul_geo.json'), 'utf-8'));

const CENTER_LNG = 126.98;
const CENTER_LAT = 37.545;
const SCALE = 85;
const RIVER_GAP = 2.5; // 강남 구역을 이만큼 아래(+z)로 밀어서 한강 공간 확보

function convert(lng, lat) {
  const x = (lng - CENTER_LNG) * SCALE;
  const z = -(lat - CENTER_LAT) * SCALE;
  return [Math.round(x * 100) / 100, Math.round(z * 100) / 100];
}

function perpDist(p, a, b) {
  const dx = b[0]-a[0], dz = b[1]-a[1];
  const len = Math.sqrt(dx*dx+dz*dz);
  if (len === 0) return Math.sqrt((p[0]-a[0])**2+(p[1]-a[1])**2);
  return Math.abs(dz*p[0]-dx*p[1]+b[0]*a[1]-b[1]*a[0])/len;
}

function simplify(pts, epsilon) {
  if (pts.length <= 3) return pts;
  let maxDist=0, maxIdx=0;
  for (let i=1; i<pts.length-1; i++) {
    const d=perpDist(pts[i],pts[0],pts[pts.length-1]);
    if (d>maxDist){maxDist=d;maxIdx=i;}
  }
  if (maxDist>epsilon) {
    const left=simplify(pts.slice(0,maxIdx+1),epsilon);
    const right=simplify(pts.slice(maxIdx),epsilon);
    return [...left.slice(0,-1),...right];
  }
  return [pts[0],pts[pts.length-1]];
}

// 강남 (한강 남쪽) 구 목록
const gangnamGus = new Set([
  '강남구','서초구','송파구','강동구','영등포구','동작구','관악구','금천구','구로구','양천구','강서구'
]);

const gameDistrictMap = {
  '마포구': { id:1, name:'홍대', grade:'A등급', rent:'₩400,000', congestion:'혼잡', tags:['Youth','Music','Art'], description:'젊음과 문화의 거리. 인디 음악과 스트릿 아트가 공존하는 MZ세대의 핫플레이스.' },
  '영등포구': { id:2, name:'여의도', grade:'B등급', rent:'₩350,000', congestion:'매우 여유', tags:['Finance','Office'], description:'금융의 중심지. 직장인 대상 런치 팝업이 유리합니다.' },
  '중구': { id:3, name:'명동', grade:'S등급', rent:'₩500,000', congestion:'매우 혼잡', tags:['Tourist','Shopping'], description:'관광객의 성지. 외국인 방문객이 가장 많은 쇼핑 거리입니다.' },
  '용산구': { id:4, name:'이태원', grade:'B등급', rent:'₩300,000', congestion:'보통', tags:['Global','Food'], description:'다국적 문화가 공존하는 거리. 이색적인 팝업에 적합한 지역입니다.' },
  '성동구': { id:5, name:'성수', grade:'S등급', rent:'₩300,000', congestion:'혼잡', tags:['Hip Vibe','Cafe Tour','Fashion'], description:'MZ세대의 놀이터이자 팝업스토어 성지. 트렌디한 카페와 편집숍이 즐비합니다.' },
  '광진구': { id:6, name:'건대', grade:'B등급', rent:'₩250,000', congestion:'여유', tags:['University','Nightlife'], description:'대학가 특유의 활기. 합리적인 임대료 대비 유동인구가 많은 지역입니다.' },
  '강남구': { id:7, name:'강남', grade:'S등급', rent:'₩600,000', congestion:'매우 혼잡', tags:['Premium','Business'], description:'대한민국 최고의 상권. 높은 임대료만큼 높은 수익 잠재력이 기대됩니다.' },
  '송파구': { id:8, name:'잠실', grade:'A등급', rent:'₩350,000', congestion:'보통', tags:['Family','Entertainment'], description:'롯데월드와 석촌호수를 중심으로 가족 단위 방문객이 많은 지역입니다.' },
};

const EPSILON = 0.15;
const gameDistricts = [];
const backgroundGus = [];

for (const feature of raw) {
  const isSouth = gangnamGus.has(feature.name);
  const coords = feature.coordinates[0];
  let pts = coords.map(([lng, lat]) => {
    const [x, z] = convert(lng, lat);
    return [x, isSouth ? z + RIVER_GAP : z]; // 강남은 아래로 밀기
  });
  pts = simplify(pts, EPSILON);
  if (pts.length>1 && pts[0][0]===pts[pts.length-1][0] && pts[0][1]===pts[pts.length-1][1]) pts=pts.slice(0,-1);

  const cx = pts.reduce((s,p)=>s+p[0],0)/pts.length;
  const cz = pts.reduce((s,p)=>s+p[1],0)/pts.length;
  const center = [Math.round(cx*100)/100, Math.round(cz*100)/100];

  const gameInfo = gameDistrictMap[feature.name];
  if (gameInfo) {
    gameDistricts.push({...gameInfo, polygon:pts, center});
  } else {
    backgroundGus.push({ name:feature.name, polygon:pts, center });
  }
}

gameDistricts.sort((a,b)=>a.id-b.id);

// 한강 북안/남안 경로 계산 (강북 구의 남쪽 경계 / 강남 구의 북쪽 경계)
// 간단하게: 한강 중심선 = 강북 최남단과 강남 최북단 사이
const riverCenterZ = RIVER_GAP / 2; // 대략적인 한강 중심 z좌표 (강북은 0 근처, 강남은 RIVER_GAP 근처)

let ts = `// Auto-generated from Seoul GeoJSON — with Han River gap
// 강남 districts offset by +${RIVER_GAP} on z-axis to create river space

export interface DistrictGeo {
  id: number;
  name: string;
  grade: string;
  rent: string;
  congestion: string;
  tags: string[];
  description: string;
  polygon: [number, number][];
  center: [number, number];
}

export interface BackgroundGu {
  name: string;
  polygon: [number, number][];
  center: [number, number];
}

export const seoulDistricts: DistrictGeo[] = ${JSON.stringify(gameDistricts, null, 2)};

export const backgroundGus: BackgroundGu[] = ${JSON.stringify(backgroundGus, null, 2)};

// River gap center z ≈ ${riverCenterZ}
export const RIVER_GAP_Z = ${RIVER_GAP};
`;

writeFileSync(resolve(__dirname, '../src/components/game/seoulDistricts.ts'), ts);
console.log(`Generated: ${gameDistricts.length} game, ${backgroundGus.length} bg`);
console.log('River gap:', RIVER_GAP);
