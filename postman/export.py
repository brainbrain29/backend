# postman/export-test-cases.py

import json
import csv
import re
import os

# 模块计数器
module_counters = {}

# 变量替换映射
VAR_REPLACEMENTS = {
    "{{adminPhone}}": "13900000000",
    "{{adminPassword}}": "admin",
    "{{testEmployeePhone}}": "13900001111",
    "{{testEmployeePassword}}": "123456",
    "{{testLeaderPhone}}": "13900002222",
    "{{testLeaderPassword}}": "123456",
    "{{testPassword}}": "123456",
    "{{newPassword}}": "newPassword456",
    "{{testDate}}": "2025-11-30",
    "{{createdEmployeeId}}": "49",
    "{{createdLeaderId}}": "50",
    "{{createdMatterId}}": "1",
    "{{testUsername}}": "testuser",
}

# 模块名称映射（中文）
MODULE_NAME_MAP = {
    "Setup": "环境准备",
    "Negative Tests - Expected Failures": "异常测试",
    "Employee Endpoints": "员工接口",
    "Admin Endpoints": "管理员接口",
    "Cleanup": "环境清理",
}

# 模块缩写映射
MODULE_ABBR = {
    "环境准备": "SETUP",
    "异常测试": "NEG",
    "员工接口": "EMP",
    "管理员接口": "ADMIN",
    "环境清理": "CLEAN",
}

def replace_variables(text):
    """替换 Postman 变量为实际值"""
    if not text:
        return text
    for var, val in VAR_REPLACEMENTS.items():
        text = text.replace(var, val)
    return text

def extract_expected_status(test_script):
    """从测试脚本中提取预期状态码"""
    if not test_script:
        return "200 OK"
    
    # 匹配 pm.response.to.have.status(xxx)
    status_match = re.search(r'to\.have\.status\((\d+)\)', test_script)
    if status_match:
        status_code = status_match.group(1)
        status_text = {
            "200": "200 OK",
            "201": "201 Created",
            "400": "400 Bad Request",
            "401": "401 Unauthorized",
            "403": "403 Forbidden",
            "404": "404 Not Found",
            "500": "500 Internal Server Error"
        }
        return status_text.get(status_code, f"{status_code}")
    
    # 匹配 oneOf([400, 500])
    oneof_match = re.search(r'to\.be\.oneOf\(\[([^\]]+)\]\)', test_script)
    if oneof_match:
        codes = oneof_match.group(1).replace(" ", "")
        return codes
    
    return "200 OK"

def get_request_param_type(request):
    """获取请求参数类型"""
    body = request.get("body", {})
    if body.get("mode") == "raw":
        return "JSON"
    elif body.get("mode") == "formdata":
        return "Form Data"
    elif body.get("mode") == "urlencoded":
        return "URL Encoded"
    
    url = request.get("url", {})
    if isinstance(url, dict) and url.get("query"):
        return "Query Params"
    
    return "无"

def get_request_params(request):
    """获取请求参数"""
    body = request.get("body", {})
    if body.get("mode") == "raw":
        raw = body.get("raw", "")
        raw = replace_variables(raw)
        try:
            parsed = json.loads(raw)
            return json.dumps(parsed, ensure_ascii=False, separators=(',', ': '))
        except:
            return raw.replace('\n', ' ').strip()
    
    url = request.get("url", {})
    if isinstance(url, dict) and url.get("query"):
        params = {q["key"]: replace_variables(q.get("value", "")) for q in url["query"] if q.get("key")}
        return json.dumps(params, ensure_ascii=False, separators=(',', ': '))
    
    return ""

def get_url_path(request):
    """获取请求 URL 路径"""
    url = request.get("url", {})
    if isinstance(url, str):
        return replace_variables(url)
    
    raw = url.get("raw", "")
    path = re.sub(r'\{\{baseUrl\}\}', '', raw)
    path = replace_variables(path)
    # 移除 query string
    path = path.split('?')[0]
    return path if path else raw

def get_precondition(module_name, item_name, request):
    """生成前置条件"""
    conditions = ["网络正常", "服务运行中"]
    
    # 检查是否需要 Token
    headers = request.get("header", [])
    for h in headers:
        if h.get("key") == "Authorization":
            auth_value = h.get("value", "")
            if "adminToken" in auth_value:
                conditions.append("已获取管理员Token")
            elif "leaderAuthToken" in auth_value:
                conditions.append("已获取团队长Token")
            elif "authToken" in auth_value:
                conditions.append("已获取用户Token")
            break
    
    # 检查是否依赖创建的数据
    url_raw = request.get("url", {})
    if isinstance(url_raw, dict):
        url_raw = url_raw.get("raw", "")
    if "createdEmployeeId" in url_raw:
        conditions.append("已创建测试员工")
    if "createdMatterId" in url_raw:
        conditions.append("已创建测试事项")
    
    return "；".join(conditions)

def get_interface_name(item_name, request):
    """获取简洁的接口名称"""
    method = request.get("method", "GET")
    url = get_url_path(request)
    
    # 简化 URL 中的 ID
    url = re.sub(r'/\d+', '/{id}', url)
    
    return f"{method} {url}"

def generate_test_id(module_cn, item_name):
    """生成测试用例 ID"""
    abbr = MODULE_ABBR.get(module_cn, "TEST")
    
    # 获取模块计数
    if abbr not in module_counters:
        module_counters[abbr] = 0
    module_counters[abbr] += 1
    
    return f"{abbr}-{module_counters[abbr]:03d}"

def get_case_name(item_name):
    """获取用例名称（简化）"""
    # 移除序号前缀
    name = re.sub(r'^\d+\.\s*', '', item_name)
    # 移除 HTTP 方法前缀
    name = re.sub(r'^(GET|POST|PUT|DELETE)\s+/[^\s]+\s*-?\s*', '', name)
    return name if name else item_name

def process_item(item, module_name, results, counter):
    """处理单个请求项"""
    # 如果是文件夹，递归处理
    if "item" in item:
        folder_name = item.get("name", module_name)
        for sub_item in item["item"]:
            counter = process_item(sub_item, folder_name, results, counter)
        return counter
    
    # 处理请求
    request = item.get("request", {})
    if not request:
        return counter
    
    counter += 1
    
    # 提取测试脚本
    test_script = ""
    events = item.get("event", [])
    for event in events:
        if event.get("listen") == "test":
            script = event.get("script", {})
            exec_lines = script.get("exec", [])
            test_script = "\n".join(exec_lines)
            break
    
    # 转换模块名称为中文
    module_cn = MODULE_NAME_MAP.get(module_name, module_name)
    
    # 构建测试用例
    test_case = {
        "ID": generate_test_id(module_cn, item.get("name", "")),
        "模块": module_cn,
        "用例名称": get_case_name(item.get("name", "")),
        "接口名称": get_interface_name(item.get("name", ""), request),
        "前置条件": get_precondition(module_cn, item.get("name", ""), request),
        "请求URL": get_url_path(request),
        "请求类型": request.get("method", "GET"),
        "请求参数类型": get_request_param_type(request),
        "请求参数": get_request_params(request),
        "预期响应状态码": extract_expected_status(test_script),
        "预期返回数据": "",
        "实际返回数据": "",
        "测试结果": "Pass"
    }
    
    results.append(test_case)
    return counter

def main():
    # 获取脚本所在目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 读取 Postman collection
    collection_path = os.path.join(script_dir, "employee-module-collection.json")
    
    with open(collection_path, 'r', encoding='utf-8') as f:
        collection = json.load(f)
    
    results = []
    counter = 0
    
    # 处理所有项目
    items = collection.get("item", [])
    for item in items:
        counter = process_item(item, "其他", results, counter)
    
    # 写入 CSV
    output_path = os.path.join(script_dir, "test-cases.csv")
    
    fieldnames = [
        "ID", "模块", "用例名称", "接口名称", "前置条件", 
        "请求URL", "请求类型", "请求参数类型", "请求参数",
        "预期响应状态码", "预期返回数据", "实际返回数据", "测试结果"
    ]
    
    with open(output_path, 'w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(results)
    
    print(f"已生成测试用例 CSV: {output_path}")
    print(f"共 {len(results)} 个测试用例")

if __name__ == "__main__":
    main()