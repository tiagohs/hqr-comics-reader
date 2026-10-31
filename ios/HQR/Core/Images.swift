import ImageIO
import Shared
import SwiftUI
import UIKit

/// Covers and pages decoded with ImageIO at the size they are shown (ADR-006: sampled, never the full bitmap), kept in a
/// small memory cache; the bytes come from the core (downloads, disk cache, then the network).
@MainActor
final class ImageStore {
    static let shared = ImageStore()
    private let cache = NSCache<NSString, UIImage>()

    init() {
        cache.totalCostLimit = 96 * 1024 * 1024
    }

    func cached(_ key: String) -> UIImage? { cache.object(forKey: key as NSString) }

    func store(_ image: UIImage, for key: String) {
        let cost = Int(image.size.width * image.size.height * image.scale * image.scale * 4)
        cache.setObject(image, forKey: key as NSString, cost: cost)
    }

    /// Decodes [data] no larger than [maxPixel] on its longest side (0 keeps the original size); with [fitWidth], the
    /// limit follows the page's proportion so a tall strip keeps that width.
    nonisolated static func decode(_ data: Data, maxPixel: CGFloat, fitWidth: CGFloat? = nil) -> UIImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, [kCGImageSourceShouldCache: false] as CFDictionary) else {
            return nil
        }
        var maxPixel = maxPixel
        if let fitWidth,
           let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
           let width = properties[kCGImagePropertyPixelWidth] as? CGFloat,
           let height = properties[kCGImagePropertyPixelHeight] as? CGFloat, width > 0 {
            maxPixel = width <= fitWidth ? 0 : fitWidth * max(1, height / width)
        }
        var options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceShouldCacheImmediately: true,
        ]
        if maxPixel > 0 { options[kCGImageSourceThumbnailMaxPixelSize] = maxPixel }
        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { return nil }
        return UIImage(cgImage: image)
    }
}

/// What an image view asks the core for.
enum ImageRequest: Equatable {
    case cover(SourceManga)
    case page(Manga, SourcePage)

    var key: String {
        switch self {
        case .cover(let manga): "cover:" + manga.listId + ":" + (manga.coverUrl ?? "")
        case .page(_, let page): "page:" + page.imageUrl
        }
    }

    @MainActor
    func load() async throws -> Data? {
        switch self {
        case .cover(let manga): try await IosBridge.shared.cover(manga: manga)
        case .page(let manga, let page): try await IosBridge.shared.page(manga: manga, page: page)
        }
    }
}

/// Loads [request] and hands the state to [content]; a failure can be retried by the caller.
@MainActor
final class ImageLoader: ObservableObject {
    enum Phase { case loading, done(UIImage), failed }

    @Published private(set) var phase: Phase = .loading
    private var current: String?

    func load(_ request: ImageRequest, maxPixel: CGFloat, fitWidth: CGFloat? = nil) async {
        let key = request.key + "@\(Int(maxPixel))w\(Int(fitWidth ?? 0))"
        if let image = ImageStore.shared.cached(key) {
            phase = .done(image)
            current = key
            return
        }
        if current != key { phase = .loading }
        current = key
        do {
            guard let data = try await request.load() else {
                phase = .failed
                return
            }
            let image = await Task.detached(priority: .userInitiated) { ImageStore.decode(data, maxPixel: maxPixel, fitWidth: fitWidth) }.value
            guard current == key else { return }
            if let image {
                ImageStore.shared.store(image, for: key)
                phase = .done(image)
            } else {
                phase = .failed
            }
        } catch {
            if current == key { phase = .failed }
        }
    }
}
