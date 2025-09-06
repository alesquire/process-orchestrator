#!/usr/bin/env python3
"""
Load Data Script - First task in the data processing pipeline
Loads data from input file and saves it in a structured format
"""

import json
import sys
import os
from datetime import datetime

def load_data(input_file, output_file):
    """Load data from input file and process it"""
    print(f"Loading data from: {input_file}")
    
    # Simulate loading data (in real scenario, this would read actual data)
    sample_data = {
        "source_file": input_file,
        "loaded_at": datetime.now().isoformat(),
        "records_count": 1000,
        "data": [
            {"id": i, "value": f"record_{i}", "timestamp": datetime.now().isoformat()}
            for i in range(1, 101)  # Sample 100 records
        ],
        "metadata": {
            "file_size": "2.5MB",
            "format": "JSON",
            "encoding": "UTF-8"
        }
    }
    
    # Save loaded data
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w') as f:
        json.dump(sample_data, f, indent=2)
    
    print(f"Data loaded successfully. Records: {sample_data['records_count']}")
    print(f"Output saved to: {output_file}")
    
    return {
        "status": "success",
        "records_loaded": sample_data['records_count'],
        "output_file": output_file,
        "file_size": sample_data['metadata']['file_size']
    }

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python load_data.py <input_file> <output_file>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    try:
        result = load_data(input_file, output_file)
        print(f"LOAD_RESULT: {json.dumps(result)}")
        sys.exit(0)
    except Exception as e:
        print(f"Error loading data: {str(e)}")
        sys.exit(1)
