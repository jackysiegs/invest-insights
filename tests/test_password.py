import bcrypt

# The hash from your database for the ii-Admin user
stored_hash = "$2a$10$lHDgZO9uNpW63zXtDn.dSOsMVR42nyRgz.nNeWGlAD5kzCralnIYC"
password = "RayJay1!"

# Test if the password matches the hash
try:
    # Encode password as bytes
    password_bytes = password.encode('utf-8')
    stored_hash_bytes = stored_hash.encode('utf-8')
    
    # Check if password matches
    if bcrypt.checkpw(password_bytes, stored_hash_bytes):
        print("✅ Password matches the hash!")
    else:
        print("❌ Password does not match the hash!")
        
    # Also test with a fresh hash to see what it should look like
    fresh_hash = bcrypt.hashpw(password_bytes, bcrypt.gensalt())
    print(f"Fresh hash for '{password}': {fresh_hash.decode('utf-8')}")
    
except Exception as e:
    print(f"Error: {e}") 