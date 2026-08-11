package com.bbs.bbs.web.response;

import java.util.List;

public record PageResult<T>(List<T> list, long total, int page, int size) {
}
