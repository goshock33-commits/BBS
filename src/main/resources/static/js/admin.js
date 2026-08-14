(function () {
    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    async function api(url, options) {
        var request = Object.assign({}, options || {});
        var headers = Object.assign({'Accept': 'application/json'}, request.headers || {});
        var csrfTokenElement = document.querySelector("meta[name='_csrf']");
        var csrfHeaderElement = document.querySelector("meta[name='_csrf_header']");
        var csrfToken = csrfTokenElement ? csrfTokenElement.content : '';
        var csrfHeader = csrfHeaderElement ? csrfHeaderElement.content : '';
        if (csrfToken && csrfHeader && request.method && request.method !== 'GET') {
            headers[csrfHeader] = csrfToken;
        }
        request.headers = headers;
        var response = await fetch(url, request);
        var payload = await response.json().catch(function () { return null; });
        if (!response.ok || !payload || payload.code !== 'SUCCESS') {
            throw new Error(payload && payload.message ? payload.message : '请求失败');
        }
        return payload.data;
    }

    function json(method, body) {
        return {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: body === undefined ? undefined : JSON.stringify(body)
        };
    }

    function iso(value) {
        return value ? new Date(value).toISOString() : '';
    }

    function formatTime(value) {
        return value ? new Date(value).toLocaleString() : '-';
    }

    window.BbsAdmin = {api: api, json: json, escapeHtml: escapeHtml, iso: iso, formatTime: formatTime};
})();
