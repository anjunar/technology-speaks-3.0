package jFx2.virtual

import app.domain.core.AbstractEntity
import app.domain.core.Data
import jFx2.core.capabilities.NodeScope

context(scope: NodeScope)
fun <T : Data<out AbstractEntity>> virtualList(
    dataProvider: RangeDataProvider<T>,
    estimateHeightPx: Int = 44,
    overscanPx: Int = 240,
    prefetchItems: Int = 80,
    renderer: context(NodeScope) (item: T?, index: Int) -> Unit
): VirtualListView<T> {
    val view = VirtualListView(
        dataProvider = dataProvider,
        estimateHeightPx = estimateHeightPx,
        overscanPx = overscanPx,
        prefetchItems = prefetchItems,
        renderer = renderer
    )
    scope.attach(view)
    return view.build()
}