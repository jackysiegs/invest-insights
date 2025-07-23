-- Clean up problematic advisor data
-- Remove the advisor with unencrypted password and null username
DELETE FROM advisor WHERE id = 5;

-- Verify the correct admin user exists
SELECT id, name, username, email, 
       CASE 
           WHEN password_hash LIKE '$2a$%' THEN 'ENCRYPTED'
           ELSE 'PLAIN_TEXT'
       END as password_status
FROM advisor 
WHERE username = 'ii-Admin';

-- Show all advisors for verification
SELECT id, name, username, email, 
       CASE 
           WHEN password_hash LIKE '$2a$%' THEN 'ENCRYPTED'
           ELSE 'PLAIN_TEXT'
       END as password_status
FROM advisor; 