import { useState } from "react";

const mockNews = {
  headline: {
    tag: "속보",
    title: "강남 지역 대규모 축제 개최, 유동인구 20% 급증 전망",
    summary:
      "오늘 오후 4시부터 강남 일대에서 버블버블 페스티벌이 개최됩니다. 주최 측은 최소 3만 명 이상의 방문객이 몰릴 것으로 예상하며, 인근 팝업스토어들의 매출 상승이 기대됩니다.",
    image: "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800&auto=format&fit=crop",
  },
  secondary: [
    {
      title: "타코야끼 트렌드 급상승, 2위 등극",
      summary: "이번 시즌 타코야끼 인기가 치솟으며 메뉴 트렌드 2위를 기록. 원가 상승 우려도.",
      image: "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600&auto=format&fit=crop",
      page: "4",
    },
    {
      title: "한파 주의보 발령, 내일 유동인구 감소",
      summary: "기상청은 내일 한파 주의보를 발령했습니다. 외출을 줄이는 시민이 늘어 유동인구 30% 감소 예상.",
      image: "https://images.unsplash.com/photo-1491002052546-bf38f186af56?w=600&auto=format&fit=crop",
      page: "7",
    },
  ],
  rankings: [
    { rank: 1, name: "강남 스트리트", change: "+12%", positive: true },
    { rank: 2, name: "홍대 거리", change: "+8%", positive: true },
    { rank: 3, name: "명동 중앙", change: "-2%", positive: false },
    { rank: 4, name: "이태원길", change: "+5%", positive: true },
    { rank: 5, name: "성수동", change: "+1%", positive: true },
  ],
  ad: {
    title: "원재료 할인 이벤트!",
    desc: "버블마트에서 전 품목 원가 15% 할인 중. 발주 전에 확인하세요.",
  },
  weather: { icon: "wb_sunny", temp: "24°C", label: "장사하기 딱 좋은 날!" },
  memo: "성수동 재고 확인하기!",
  day: 3,
  season: 12,
};

export default function NewsPage() {
  const [expandedCard, setExpandedCard] = useState<number | null>(null);

  return (
    <div
      className="min-h-screen w-full font-cozy-display text-cozy-ink bg-cover bg-center bg-fixed relative"
      style={{
        backgroundImage:
          "url('https://images.unsplash.com/photo-1516216628859-9bccecab13ca?q=80&w=2669&auto=format&fit=crop')",
      }}
    >
      {/* Wood overlay */}
      <div className="absolute inset-0 bg-[rgba(60,40,30,0.25)]" />

      {/* Glass Navbar */}
      <header className="sticky top-0 z-50 flex items-center justify-between border-b border-white/20 bg-white/70 backdrop-blur-md px-6 py-3 shadow-sm lg:px-10 relative">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-3">
            <div className="size-8 rounded-full bg-cozy-primary flex items-center justify-center text-white shadow-lg">
              <span className="material-symbols-outlined text-[20px]">bubble_chart</span>
            </div>
            <h2 className="text-cozy-ink text-xl font-black tracking-tight font-cozy-serif italic">
              The Daily Bubble
            </h2>
          </div>
          <nav className="hidden md:flex items-center gap-6">
            {["Home", "News", "Rankings", "Profile"].map((item) => (
              <a
                key={item}
                className={`text-sm font-bold uppercase tracking-wider transition-colors ${
                  item === "News"
                    ? "text-cozy-primary border-b-2 border-cozy-primary"
                    : "text-cozy-ink/70 hover:text-cozy-primary"
                }`}
                href="#"
              >
                {item}
              </a>
            ))}
          </nav>
        </div>
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex items-center rounded-full bg-white/50 border border-white/40 px-3 py-1.5 w-64">
            <span className="material-symbols-outlined text-cozy-dusty-rose">search</span>
            <input
              className="w-full bg-transparent border-none px-2 py-0 text-sm placeholder:text-cozy-ink/40 focus:ring-0 focus:outline-none"
              placeholder="Search headlines..."
            />
          </div>
          <div className="size-10 rounded-full bg-cozy-primary/10 border-2 border-white shadow-lg flex items-center justify-center overflow-hidden">
            <span className="material-symbols-outlined text-cozy-primary">person</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="relative z-10 w-full max-w-[1440px] mx-auto p-4 md:p-8 lg:p-12 grid grid-cols-1 lg:grid-cols-12 gap-8 items-start backdrop-blur-[2px]">
        {/* ── Left Sidebar ── */}
        <aside className="hidden lg:flex lg:col-span-2 flex-col gap-6 sticky top-24">
          {/* Weather Coaster */}
          <div className="aspect-square rounded-full bg-[#f0eadd] shadow-cozy-float border-4 border-[#e6dec9] flex flex-col items-center justify-center p-4 text-center -rotate-[5deg]">
            <span className="material-symbols-outlined text-4xl text-cozy-sage mb-1">
              {mockNews.weather.icon}
            </span>
            <span className="font-cozy-serif font-bold text-2xl text-cozy-ink/80">
              {mockNews.weather.temp}
            </span>
            <span className="text-xs font-cozy-hand text-cozy-dusty-rose -rotate-[2deg] mt-1">
              {mockNews.weather.label}
            </span>
          </div>

          {/* Archive Button */}
          <div className="h-48 w-full bg-gradient-to-br from-gray-700 to-gray-900 rounded-[2rem] shadow-xl flex items-center justify-center relative overflow-hidden group cursor-pointer rotate-[2deg]">
            <div
              className="absolute inset-0 opacity-20"
              style={{
                backgroundImage:
                  "url('https://www.transparenttextures.com/patterns/leather.png')",
              }}
            />
            <div className="relative z-10 text-white/80 text-center">
              <span className="material-symbols-outlined text-4xl group-hover:text-white transition-colors">
                menu_book
              </span>
              <p className="text-xs mt-2 font-medium tracking-widest uppercase opacity-60">
                Archive
              </p>
            </div>
          </div>
        </aside>

        {/* ── Center: Newspaper ── */}
        <section className="lg:col-span-7 flex flex-col gap-6">
          {/* Main Newspaper */}
          <div className="bg-cozy-paper shadow-cozy-paper rounded-sm overflow-hidden relative hover:-translate-y-1 transition-transform duration-300">
            {/* Paper texture */}
            <div
              className="absolute inset-0 opacity-50 mix-blend-multiply pointer-events-none"
              style={{
                backgroundImage:
                  "url('https://www.transparenttextures.com/patterns/cream-paper.png')",
              }}
            />
            {/* Fold line */}
            <div className="absolute top-1/2 left-0 w-full h-px bg-gradient-to-r from-transparent via-gray-300 to-transparent z-20" />
            <div className="absolute top-1/2 left-0 w-full h-8 bg-gradient-to-b from-black/5 to-transparent z-10 pointer-events-none" />

            <div className="p-8 pb-12 border-b border-cozy-ink/10 relative z-10">
              {/* Masthead */}
              <div className="flex justify-between items-end border-b-4 border-cozy-ink mb-6 pb-2">
                <div>
                  <h1 className="font-cozy-serif text-5xl md:text-7xl font-black text-cozy-ink tracking-tight uppercase">
                    Daily Bubble
                  </h1>
                  <p className="font-cozy-serif italic text-cozy-ink/60 mt-2">
                    Vol. {mockNews.season} · Your Morning Brew of Updates ·{" "}
                    <span className="text-cozy-primary font-bold not-italic text-xs tracking-wider">
                      DAY {mockNews.day}
                    </span>
                  </p>
                </div>
                <div className="hidden md:block text-right">
                  <span className="block font-cozy-display font-bold text-3xl text-cozy-sage">
                    $0.50
                  </span>
                </div>
              </div>

              {/* Headline */}
              <article className="grid md:grid-cols-2 gap-8 cursor-pointer group">
                <div className="flex flex-col gap-4">
                  <span className="inline-block bg-cozy-primary text-white text-[10px] font-bold uppercase tracking-widest px-2 py-1 w-fit">
                    {mockNews.headline.tag}
                  </span>
                  <h2 className="font-cozy-serif text-3xl md:text-4xl font-bold leading-tight group-hover:text-cozy-primary transition-colors">
                    {mockNews.headline.title}
                  </h2>
                  <p className="text-cozy-ink/70 leading-relaxed text-sm border-l-2 border-cozy-sage pl-4">
                    {mockNews.headline.summary}
                  </p>
                  <div className="mt-2 flex items-center gap-2 text-xs font-bold text-cozy-dusty-rose uppercase tracking-wider">
                    <span>Read Full Story</span>
                    <span className="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">
                      arrow_forward
                    </span>
                  </div>
                </div>
                <div className="relative aspect-[4/3] w-full overflow-hidden grayscale group-hover:grayscale-0 transition-all duration-500 rounded-sm border border-cozy-ink/20 shadow-inner">
                  <img
                    src={mockNews.headline.image}
                    alt=""
                    className="object-cover w-full h-full mix-blend-multiply"
                  />
                  <div className="absolute bottom-0 right-0 bg-white/90 backdrop-blur px-2 py-1 text-[10px] font-mono border-t border-l border-cozy-ink/20">
                    Fig 1.A — Festival
                  </div>
                </div>
              </article>
            </div>
          </div>

          {/* Secondary Stories */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {mockNews.secondary.map((story, i) => (
              <div
                key={i}
                className={`bg-cozy-paper shadow-cozy-paper p-6 relative group cursor-pointer ${
                  i % 2 === 0 ? "-rotate-1" : "rotate-1"
                } hover:rotate-0 transition-transform duration-300`}
                onClick={() => setExpandedCard(expandedCard === i ? null : i)}
              >
                <div
                  className="absolute inset-0 opacity-50 mix-blend-multiply pointer-events-none"
                  style={{
                    backgroundImage:
                      "url('https://www.transparenttextures.com/patterns/cream-paper.png')",
                  }}
                />
                <div className="relative z-10 flex flex-col h-full">
                  <div className="h-40 w-full mb-4 overflow-hidden border border-cozy-ink/10">
                    <img
                      src={story.image}
                      alt=""
                      className="object-cover w-full h-full sepia-[.3] group-hover:sepia-0 transition-all"
                    />
                  </div>
                  <h3 className="font-cozy-serif text-2xl font-bold leading-tight mb-2 group-hover:text-cozy-primary">
                    {story.title}
                  </h3>
                  <p className="text-sm text-cozy-ink/70 mb-4 flex-grow">{story.summary}</p>
                  <div className="border-t border-dashed border-cozy-ink/20 pt-3 flex justify-between items-center">
                    <span className="font-cozy-hand text-cozy-sage text-sm">p. {story.page}</span>
                    <span className="material-symbols-outlined text-cozy-ink/40 group-hover:text-cozy-primary">
                      visibility
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Ad Strip */}
          <div className="bg-[#f0eadd] p-4 border border-cozy-ink/10 flex items-center justify-between shadow-sm relative overflow-hidden">
            <div className="absolute -right-4 -top-4 text-[100px] text-black/5 rotate-12 select-none font-cozy-serif font-black">
              SALE
            </div>
            <div className="relative z-10">
              <h4 className="font-cozy-serif font-bold text-lg">{mockNews.ad.title}</h4>
              <p className="text-xs text-cozy-ink/60">{mockNews.ad.desc}</p>
            </div>
            <button className="relative z-10 bg-cozy-ink text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-cozy-primary transition-colors">
              Shop Now
            </button>
          </div>
        </section>

        {/* ── Right Sidebar ── */}
        <aside className="lg:col-span-3 flex flex-col gap-8 relative sticky top-24">
          {/* Coffee Cup */}
          <div className="absolute -right-12 -top-24 z-20 pointer-events-none select-none hidden xl:block">
            <div className="w-48 h-48 rounded-full bg-[#f0eadd] shadow-cozy-float border-4 border-[#e6dec9] flex items-center justify-center">
              <span className="material-symbols-outlined text-7xl text-cozy-wood-dark/30">
                coffee
              </span>
            </div>
          </div>

          {/* Notebook Rankings */}
          <div className="relative bg-white rounded-lg shadow-cozy-float mt-12 rotate-2 hover:rotate-0 transition-transform">
            {/* Spiral Binding */}
            <div className="absolute -top-3 left-0 w-full h-8 flex justify-evenly z-20">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="w-2 h-6 bg-zinc-400 rounded-full shadow-md" />
              ))}
            </div>
            <div
              className="p-6 pt-10 min-h-[400px]"
              style={{
                backgroundImage: "linear-gradient(#e5e7eb 1px, transparent 1px)",
                backgroundSize: "100% 2rem",
              }}
            >
              <h3 className="font-cozy-hand text-3xl text-cozy-primary mb-6 text-center -rotate-2">
                Top Bubbles
              </h3>
              <ul className="flex flex-col gap-4 pl-2">
                {mockNews.rankings.map((r) => (
                  <li key={r.rank} className="flex items-center gap-3">
                    <span className="font-cozy-hand text-xl text-cozy-dusty-rose w-6">
                      {r.rank}.
                    </span>
                    <div className="flex-1 border-b border-cozy-ink/10 pb-1 flex justify-between items-baseline">
                      <span className="font-cozy-hand text-lg text-cozy-ink/80">{r.name}</span>
                      <span
                        className={`text-xs font-mono px-1 rounded font-bold ${
                          r.positive
                            ? "bg-cozy-sage/20 text-cozy-sage"
                            : "bg-red-100 text-red-400"
                        }`}
                      >
                        {r.change}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>

              {/* Sticky note */}
              <div className="mt-8 -rotate-[3deg]">
                <div className="bg-yellow-200/80 p-3 shadow-sm inline-block max-w-[80%]">
                  <p className="font-cozy-hand text-sm text-cozy-ink/70">{mockNews.memo}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Pen */}
          <div className="w-64 h-4 bg-gradient-to-r from-black to-gray-700 rounded-full shadow-lg rotate-45 translate-x-12 translate-y-4 relative opacity-90 hidden lg:block">
            <div className="absolute right-0 top-0 h-full w-12 bg-cozy-primary rounded-r-full" />
            <div className="absolute right-12 top-0 h-full w-2 bg-gray-400" />
          </div>
        </aside>
      </main>
    </div>
  );
}
