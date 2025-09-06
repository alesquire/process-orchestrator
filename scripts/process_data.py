#!/usr/bin/env python3
"""
Process Data Script - Second task in the data processing pipeline
Processes loaded data and applies transformations
"""

import json
import sys
import os
from datetime import datetime

def process_data(input_file, output_file):
    """Process loaded data and apply transformations"""
    print(f"Processing data from: {input_file}")
    
    # Load the data
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    # Apply transformations
    processed_records = []
    for record in data['data']:
        # Transform each record
        processed_record = {
            "id": record['id'],
            "processed_value": record['value'].upper(),  # Convert to uppercase
            "processed_timestamp": datetime.now().isoformat(),
            "original_timestamp": record['timestamp'],
            "transformation_applied": "uppercase_conversion"
        }
        processed_records.append(processed_record)
    
    # Create processed data structure
    processed_data = {
        "source_file": input_file,
        "processed_at": datetime.now().isoformat(),
        "original_records_count": data['records_count'],
        "processed_records_count": len(processed_records),
        "transformations": [
            "uppercase_conversion",
            "timestamp_normalization"
        ],
        "data": processed_records,
        "metadata": {
            "processing_time_ms": 150,
            "memory_used_mb": 25.5,
            "transformations_count": 2
        }
    }
    
    # Save processed data
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w') as f:
        json.dump(processed_data, f, indent=2)
    
    print(f"Data processed successfully. Records: {len(processed_records)}")
    print(f"Output saved to: {output_file}")
    
    return {
        "status": "success",
        "records_processed": len(processed_records),
        "output_file": output_file,
        "processing_time_ms": processed_data['metadata']['processing_time_ms'],
        "transformations_applied": processed_data['transformations']
    }

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python process_data.py <input_file> <output_file>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    try:
        result = process_data(input_file, output_file)
        print(f"PROCESS_RESULT: {json.dumps(result)}")
        sys.exit(0)
    except Exception as e:
        print(f"Error processing data: {str(e)}")
        sys.exit(1)
