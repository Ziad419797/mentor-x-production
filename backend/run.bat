@echo off
:: استخدام النقطة يعني "الملف اللي واقف جنبك في نفس الفولدر"
set KEY_PATH=".\educore-key.pem"
set JAR_NAME="educore-0.0.1-SNAPSHOT.jar"
set SERVER="ubuntu@98.94.45.209"

set DB_URL="jdbc:postgresql://ep-patient-flower-alujni3x-pooler.c-3.eu-central-1.aws.neon.tech/neondb?sslmode=require"
set DB_USER="neondb_owner"
set DB_PASS="npg_D2EYvIr9LlVb"

echo 🛠️ Step 1: Cleaning and Building Fresh JAR...
call mvnw clean package -DskipTests -T 1C

echo 🚀 Step 2: Uploading to Amazon AWS...
:: الـ scp هياخد المفتاح من نفس الفولدر دلوقتي
scp -C -i %KEY_PATH% target/%JAR_NAME% %SERVER%:/home/ubuntu/

echo 🧹 Step 3: Killing Old Process and Restarting...
ssh -i %KEY_PATH% %SERVER% "sudo pkill -9 java; sleep 2; nohup java -jar /home/ubuntu/%JAR_NAME% --spring.datasource.url=%DB_URL% --spring.datasource.username=%DB_USER% --spring.datasource.password=%DB_PASS% --server.port=8081 > nohup.out 2>&1 &"

echo ✅ DONE! Update is live.
echo 🔍 To see logs: ssh -i %KEY_PATH% %SERVER% "tail -f nohup.out"