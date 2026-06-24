# Sismik İzleme Konsolu — `eq-web`

`earthquake-monitoring` API'si için React + Vite + MapLibre GL JS ön yüzü. Tek backend ön
koşulu olan CORS backend tarafında zaten hazır (`app.cors.allowed-origins`, varsayılan
`localhost:5173`).

## Çalıştırma

```bash
# 1) backend'i ayağa kaldır (ayrı terminal) — standalone monolit :8081
cd ../earthquake-monitoring
mvn spring-boot:run

# 2) ön yüz
cd eq-web
npm install
npm run dev          # http://localhost:5173
```

Dev sunucusu `/api` isteklerini `http://localhost:8081`'e proxy'ler (CORS derdi yok).
Farklı bir backend adresi için: `VITE_API_PROXY=http://host:port npm run dev`.

Üretim derlemesi:

```bash
npm run build        # tsc --noEmit + vite build -> dist/
npm run preview
```

Ayrı origin'de yayınlarken API kökünü `.env` ile verin:

```
VITE_API_BASE=https://api.example.com
```

## Demo hesabı

| Kullanıcı | Parola     | Rol   | Yetki                |
|-----------|------------|-------|----------------------|
| `admin`   | `admin123` | ADMIN | Her şey + olay silme |

Kimlik doğrulama backend'deki `DemoUserStore` ile yapılır ve şu an **yalnızca bu tek hesap**
tanımlıdır (`scientist`/`viewer` hesapları artık kodda yok). Rol bazlı yetkiler backend'in
`SecurityConfig` kurallarıyla birebir aynı: UI yalnızca rolün izin verdiği eylemleri gösterir,
sunucu da ikinci savunma hattı olarak uygular.

## Görünümler

- **İzleme** — risk renkli olay haritası (MapLibre GL JS, gerçek tile'lı altlık) ve metrik
  şeridi; tam olay listesi **Tablo** görünümündedir. Bilim insanı/yönetici akışları içe aktarır;
  yönetici olay siler.
- **Algılama** — listeden gerçek bir deprem seçilir; büyüklüğüyle orantılı sentetik bir
  sinyal üretilip `/api/detection/analyze`'a gönderilir. Sistem STA/LTA ile tetiklenmeyi
  kontrol eder, tepe genlikten büyüklüğü kestirir ve **gerçek büyüklükle karşılaştırır**.
  Sinyal genliği büyüklükle ~10^M ölçeklendiği için tahmin gerçek değeri yakalar. İmza
  öğesi: canlı sismograf izi.
- **Değerlendirme** — afet türü + şiddet ile `/api/disasters/assess`; risk ve tavsiye.
- **Rapor** — özet metrikler, risk dağılımı çubuğu, izleme döngüsü, metin/Markdown
  çıktısı ve manuel olay ekleme.

## API eşlemesi

Tüm yollar backend ile aynı; yanıt alan adları DTO'larla birebir (`src/api/types.ts`):

| UI eylemi              | İstek                                  |
|------------------------|----------------------------------------|
| Giriş                  | `POST /api/auth/login`                 |
| Olay listesi/harita    | `GET /api/earthquakes`                 |
| Özet rapor             | `GET /api/reports`                     |
| Biçimli rapor          | `GET /api/reports/render?format=`      |
| İstatistik şeridi       | `GET /api/stats` (platform profilinde yoksa atlanır) |
| Sinyal analizi         | `POST /api/detection/analyze`          |
| Afet değerlendirme     | `POST /api/disasters/assess`           |
| Akış içe aktar         | `POST /api/feeds/import`               |
| İzleme döngüsü          | `POST /api/monitoring/cycle`           |
| Manuel olay ekle       | `POST /api/earthquakes`                |
| Olay sil (admin)       | `DELETE /api/earthquakes/{id}`         |

JWT `Authorization: Bearer <token>` başlığında gider (çerez yok). Token süresi
dolunca veya herhangi bir 401'de oturum temizlenir, giriş ekranına dönülür.

## Tasarım

Jenerik AI görünümünden kaçınıp bilimsel **enstrüman / kontrol odası** estetiği:
koyu sol araç rayı, sıcak-açık çalışma alanı, sismik teal vurgu, monospace veri
okumaları (IBM Plex Mono), Space Grotesk başlıklar. Sayfanın en yüksek sesli rengi
risk seviyeleridir (LOW/MEDIUM/HIGH/CRITICAL) — verinin kendi dili. İmza bileşeni,
algılama panelinde canlı sismograf izi çizen SVG.

## Yapı

```
src/
  api/        types · client (fetch + auth + hata) · endpoints
  auth/       AuthContext (token, rol, yetki, süre)
  lib/        risk · format · geo · region · signal (sentetik sinyal üreteci)
  components/ Toast · ui · Seismograph · SeismicMap · EventTable · AdminLoginModal
              · DataContext · RegionContext
  views/      RegionSelectView · Shell · Monitor · Detection · Disaster · Report
  index.css   tasarım sistemi
```

Uygulama açılışta bir bölge seçtirir (`RegionSelectView`); seçilen bölge kaynak/kıta
filtrelerini ve varsayılanları belirler.
