import csv

def cleanup_csv():
    csv_path = r"d:\Projects\pandora\backend\postman\test-cases.csv"
    
    # 读取所有行
    with open(csv_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # 保留除了最后5行以外的所有行（刚才添加的无效行）
    # 但保留标题行
    valid_lines = lines[:-5]  # 删除最后5行
    
    # 写回文件
    with open(csv_path, 'w', encoding='utf-8') as f:
        f.writelines(valid_lines)
    
    print(f"已清理CSV文件，删除了最后5行无效数据")

if __name__ == "__main__":
    cleanup_csv()