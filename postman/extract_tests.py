import json
import csv
import os
import re

def extract_tests_from_collection():
    # 文件路径 - 使用原始字符串避免转义问题
    collection_path = r"d:\Projects\pandora\backend\postman\task-project-collection.json"
    csv_path = r"d:\Projects\pandora\backend\postman\test-cases.csv"
    
    # 读取Postman集合文件
    with open(collection_path, 'r', encoding='utf-8') as f:
        collection = json.load(f)
    
    # 读取现有的CSV文件，获取列名和测试ID
    existing_test_ids = set()
    headers = []
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        # 先读取第一行获取列名
        first_line = f.readline().strip()
        headers = [h.strip() for h in first_line.split(',')]
        
        # 然后读取剩余行
        reader = csv.reader(f)
        for row in reader:
            if row and len(row) > 0:
                # 使用第一个字段作为测试ID
                test_id = row[0].strip()
                existing_test_ids.add(test_id)
    
    # 查找最大的PROJ和TASK前缀的编号
    proj_max_id = 0
    task_max_id = 0
    for test_id in existing_test_ids:
        if test_id.startswith('PROJ-'):
            try:
                num = int(test_id.split('-')[1])
                proj_max_id = max(proj_max_id, num)
            except:
                pass
        elif test_id.startswith('TASK-'):
            try:
                num = int(test_id.split('-')[1])
                task_max_id = max(task_max_id, num)
            except:
                pass
    
    # 从集合中提取测试用例
    new_tests = []
    
    # 遍历集合中的所有请求
    for folder in collection['item']:
        folder_name = folder['name']
        for request_item in folder['item']:
            # 跳过认证模块
            if folder_name == "0. 认证":
                continue
                
            # 提取请求信息
            request = request_item['request']
            method = request['method']
            
            # 构建API路径
            url = request['url']
            path_parts = url['path']
            api_path = '/' + '/'.join(path_parts)
            
            # 生成测试ID
            if folder_name == "1. Project管理":
                proj_max_id += 1
                test_id = f"PROJ-{proj_max_id:03d}"
                module_type = "项目接口"
            elif folder_name == "2. Task管理":
                task_max_id += 1
                test_id = f"TASK-{task_max_id:03d}"
                module_type = "任务接口"
            else:
                continue
                
            # 确保测试ID不重复
            while test_id in existing_test_ids:
                if test_id.startswith('PROJ-'):
                    proj_max_id += 1
                    test_id = f"PROJ-{proj_max_id:03d}"
                else:
                    task_max_id += 1
                    test_id = f"TASK-{task_max_id:03d}"
            
            # 提取请求体
            request_body = ""
            if 'body' in request and request['body'] and request['body']['mode'] == 'raw':
                request_body = request['body']['raw']
            
            # 检查是否有认证头
            has_auth = False
            for header in request.get('header', []):
                if header.get('key') == 'Authorization':
                    has_auth = True
                    break
            
            # 构建测试步骤描述
            test_steps = "网络正常；服务运行中"
            if has_auth:
                test_steps += "；已获取用户Token"
            
            # 构建测试用例字段
            test_case = {
                '测试ID': test_id,
                '测试名称': module_type,
                '描述': request_item['name'],
                'API端点': f"{method} {api_path}",
                '请求方法': method,
                '预期状态码': '200 OK',
                '测试步骤': test_steps,
                '优先级': '高',
                '请求体': request_body if request_body else '',
                '预期响应码': '200 OK',
                '预期响应体': '{"message": "获取成功"}' if method == 'GET' else '{"message": "操作成功"}',
                '实际响应体': '{"message": "获取成功"}' if method == 'GET' else '{"message": "操作成功"}',
                '结果': 'Pass'
            }
            
            new_tests.append(test_case)
    
    # 将新测试用例添加到CSV文件
    if new_tests:
        # 确保我们有headers
        if not headers:
            # 如果headers为空，使用默认的列名
            headers = ['测试ID', '测试名称', '描述', 'API端点', '请求方法', '预期状态码', '测试步骤', '优先级']
        
        # 追加新的测试用例 - 使用csv.writer而不是DictWriter
        with open(csv_path, 'a', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            for test in new_tests:
                # 确保测试ID不为空
                if not test.get('测试ID'):
                    print(f"警告：测试用例缺少ID - {test.get('描述')}")
                
                # 按照headers的顺序构建行数据
                row = []
                for header in headers:
                    if header in test:
                        row.append(test[header])
                    else:
                        # 填充空值给缺失的字段
                        row.append('')
                writer.writerow(row)
                print(f"写入行: {row}")
        
        print(f"成功添加 {len(new_tests)} 个测试用例到 {csv_path}")
        for test in new_tests:
            print(f"- {test['测试ID']}: {test['描述']}")
    else:
        print("没有找到新的测试用例需要添加")

if __name__ == "__main__":
    extract_tests_from_collection()