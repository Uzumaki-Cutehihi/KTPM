export default function Home() {
  return (
    <section className="relative overflow-hidden">
      <div className="max-w-6xl mx-auto px-4 py-16">
        <div className="grid md:grid-cols-2 gap-8 items-center">
          <div>
            <h1 className="text-4xl md:text-5xl font-bold tracking-tight text-gray-900">BookVault</h1>
            <p className="mt-4 text-gray-600">Quản lý thư viện trực tuyến với trải nghiệm hiện đại, nhanh và đẹp.</p>
            <div className="mt-6 flex gap-3">
              <a href="/books" className="px-4 py-2 rounded-md bg-primary-600 text-white hover:bg-primary-700">Khám phá sách</a>
              <a href="/search" className="px-4 py-2 rounded-md border border-gray-300 hover:bg-gray-100">Tìm kiếm</a>
            </div>
          </div>
          <div className="hidden md:block">
            <div className="rounded-xl bg-gradient-to-tr from-primary-100 to-primary-200 p-16" />
          </div>
        </div>
      </div>
    </section>
  )
}
