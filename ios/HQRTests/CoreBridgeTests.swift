import Shared
import XCTest
@testable import HQR

/// The Kotlin core as the app sees it through the Shared framework.
@MainActor
final class CoreBridgeTests: XCTestCase {
    override func setUp() {
        Core.start()
    }

    func test_FEAT060_library_view_model_starts_loading() {
        let model = ScreenModel(Core.graph.libraryViewModel()) { Screen<LibraryViewModel.State>(viewModel: $0, flow: $0.state) }
        XCTAssertNotNil(model.state)
    }

    func test_RF75_backup_bytes_cross_the_bridge_unchanged() {
        let bytes = Core.graph.bytes(data: Data([1, 2, 3, 250]))
        XCTAssertEqual(Core.graph.data(bytes: bytes), Data([1, 2, 3, 250]))
    }

    func test_RF115_the_star_reaches_the_feed_and_comes_back() async throws {
        let first = try await Core.graph.toggleFavoriteSource(sourceId: "hqnow")
        XCTAssertTrue(["added", "removed", "full"].contains(first))
        if first == "added" {
            let second = try await Core.graph.toggleFavoriteSource(sourceId: "hqnow")
            XCTAssertEqual(second, "removed")
        }
    }

    func test_RF116_feed_age_reads_minutes() {
        let tenMinutesAgo = Int64(Date().addingTimeInterval(-600).timeIntervalSince1970 * 1000)
        XCTAssertEqual(ago(tenMinutesAgo), L("time_minutes_ago", 10))
        XCTAssertEqual(ago(nil), L("time_just_now"))
    }

    func test_RF81_onboarding_saves_the_languages_and_does_not_come_back() async throws {
        try await Core.graph.finishOnboarding(languages: ["en", "pt-br"], webtoon: false)
        XCTAssertEqual((Core.graph.preferences().onboarded.value as? KotlinBoolean)?.boolValue, true)
        let value = Core.graph.preferences().contentLanguages.value
        let saved = (value as? Set<String>) ?? Set((value as? NSSet)?.compactMap { $0 as? String } ?? [])
        XCTAssertEqual(saved, ["en", "pt-br"])
    }
}
