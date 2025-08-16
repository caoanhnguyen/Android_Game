package com.mygdx.td.render;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapGroupLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

/**
 * Custom renderer: expose begin/end batch for manual layer rendering.
 * Tuyệt đối không dùng batch ngoài truyền vào!
 */
public class OrderedOrthogonalTiledMapRenderer extends OrthogonalTiledMapRenderer {
    public OrderedOrthogonalTiledMapRenderer(TiledMap map, float unitScale) { super(map, unitScale); }
    public OrderedOrthogonalTiledMapRenderer(TiledMap map) { super(map); }

    public void beginCustom() { this.batch.begin(); }
    public void endCustom() { this.batch.end(); }

    public void renderLayer(MapLayer layer) {
        if (layer == null || !layer.isVisible()) return;
        if (layer instanceof MapGroupLayer) {
            MapLayers children = ((MapGroupLayer) layer).getLayers();
            for (int i = 0; i < children.size(); i++) {
                renderLayer(children.get(i));
            }
            return;
        }
        if (layer instanceof TiledMapTileLayer) {
            renderTileLayer((TiledMapTileLayer) layer);
        } else if (layer instanceof TiledMapImageLayer) {
            renderImageLayer((TiledMapImageLayer) layer);
        } else {
            if (layer.getObjects() != null && layer.getObjects().getCount() > 0) {
                renderObjects(layer);
            }
        }
    }
}
