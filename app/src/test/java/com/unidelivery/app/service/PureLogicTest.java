package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 纯逻辑单测：200 分批边界、brand_info 契约映射、MD5 已知向量。 */
class PureLogicTest {

    @Test
    void chunkSplitsAtPlatformLimit() {
        List<String> videos = IntStream.range(0, 250).mapToObj(i -> "v" + i).toList();
        List<List<String>> chunks = ProjectService.chunk(videos, 200);
        assertEquals(2, chunks.size());
        assertEquals(200, chunks.get(0).size());
        assertEquals(50, chunks.get(1).size());
        assertEquals(List.of(), ProjectService.chunk(List.of(), 200));
        assertEquals(1, ProjectService.chunk(videos.subList(0, 200), 200).size());
    }

    @Test
    void brandInfoMappingFollowsContract() throws Exception {
        String json = """
                {"merge_brand_id":11,"merge_brand_name":"品牌A","available_status":"VALID",
                 "yuntu_brand_detail":{"outer_brand_id":22,
                   "sub_brand_details":[{"outer_brand_id":33,"brand_full_name":"子品牌A1"}]}}
                """;
        JsonNode brand = new ObjectMapper().readTree(json);
        Map<String, Object> info = PreCheckService.assembleBrandInfo(brand);
        assertEquals(11L, info.get("cdp_brand_id"));
        assertEquals("品牌A", info.get("cdp_brand_name"));
        assertEquals(22L, info.get("brand_name_id"));
        assertEquals(List.of("子品牌A1"), info.get("sub_brand_names"));
        assertEquals(List.of("33"), info.get("sub_brand_name_ids"));
        assertTrue(!info.containsKey("yuntu_category_id"), "来源接口未核实的字段不得臆造");
    }

    @Test
    void md5KnownVector() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", UploadService.md5OfString("abc"));
    }
}
