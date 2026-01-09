C4Component
title Stream Aggregator - Components
Container_Boundary(b, "Stream Aggregator") {
  Component(parser, "Event Parser", "Decode/validate schema")
  Component(dedup, "Deduplication", "Idempotency keys / Bloom / state store")
  Component(window, "Windowing", "Tumbling/Sliding windows + watermark")
  Component(counter, "Per-ad Counter", "Stateful aggregation per ad_id")
  Component(topn, "Top-N Calculator", "Heap/partial reduce + merge")
  Component(writer, "DB Writer", "Idempotent upserts + offset commit")
}
Rel(parser, dedup, "events")
Rel(dedup, window, "clean events")
Rel(window, counter, "bucketed")
Rel(counter, topn, "partial aggregates")
Rel(topn, writer, "top-N + aggregates")
