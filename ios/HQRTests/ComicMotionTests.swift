import XCTest
@testable import HQR

/// RF-118: the same motion rules as Android (ComicMotionTest).
final class ComicMotionTests: XCTestCase {
    func test_RF118_every_movement_fits_between_150_and_480_ms() {
        for seconds in [ComicMotion.panel, ComicMotion.cascade, ComicMotion.sketch, ComicMotion.writeSeconds(7)] {
            XCTAssertTrue((0.15...0.48).contains(seconds), "\(seconds)")
        }
    }

    func test_RF118_a_list_cascades_only_over_its_first_rows() {
        XCTAssertEqual(ComicMotion.cascadeDelay(0, reduce: false), 0)
        XCTAssertEqual(ComicMotion.cascadeDelay(40, reduce: false), ComicMotion.cascadeDelay(ComicMotion.cascadeRows, reduce: false))
    }

    func test_RF118_reduce_motion_leaves_only_the_fade() {
        XCTAssertEqual(ComicMotion.cascadeDelay(5, reduce: true), 0)
        XCTAssertEqual(ComicMotion.duration(ComicMotion.panel, reduce: true), ComicMotion.fade)
        XCTAssertEqual(ComicMotion.letterAlpha(index: 6, count: 7, progress: 0, reduce: true), 1)
    }

    func test_RF118_the_vertical_title_writes_from_top_to_bottom() {
        XCTAssertGreaterThan(ComicMotion.letterAlpha(index: 0, count: 6, progress: 0.5, reduce: false),
                             ComicMotion.letterAlpha(index: 5, count: 6, progress: 0.5, reduce: false))
        XCTAssertEqual(ComicMotion.letterAlpha(index: 5, count: 6, progress: 0, reduce: false), 0)
        XCTAssertEqual(ComicMotion.letterAlpha(index: 5, count: 6, progress: 1, reduce: false), 1)
    }
}
