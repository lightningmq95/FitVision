import happybase

def print_table_contents():
    try:
        connection = happybase.Connection('localhost')
        print(connection.tables())
        table = connection.table('image_metadata')
        
        print("\nScanning HBase table 'image_metadata':")
        print("-" * 50)
        print("ROW KEY | CATEGORY | COLOR")
        print("-" * 50)
        
        for key, data in table.scan():
            # Decode row key and values
            print(key, data)
            # row_key = key.decode('utf-8')
            # category = data.get(b'metadata:category', b'N/A').decode('utf-8')
            # color = data.get(b'metadata:color', b'N/A').decode('utf-8')
            
            # print(f"{row_key} | {category} | {color}")
            
        connection.close()
        
    except Exception as e:
        print(f"Error: {str(e)}")

if __name__ == "__main__":
    print_table_contents()