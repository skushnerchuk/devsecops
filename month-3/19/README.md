##  Обеспечение безопасности CI/CD тулчейна и DevOps процесса

Запустим процесс поиска 

```bash
trufflehog git https://github.com/OtusTeam/DevSecOps_secret-finding.git | tee -a log.txt
```

В результате проверки найдено 38 

**Detector Type: JDBC**
Raw result: jdbc:postgresql://webgoat_db:5432/webgoat?user=webgoat&password=webgoat
Commit: bc0d803123f5cd5e3f3e857398b8f2b0c4aad5b9
File: docker-compose-postgres.yml
Line: 10

**Detector Type: PrivateKey**
Raw result: -----BEGIN PRIVATE KEY-----
...
-----END PRIVATE KEY-----
Commit: 34f1faad298b13e515a62330f593dac142506789
File: webgoat-server/privatekey.key
Line: 1

**Detector Type: SQLServer**
Raw result: Mot de passe
Login=Login
RequiredFields=Champs obligatoires
WeakAuthenticationCookiePleaseSignIn=Veuillez vous connecter \u00e0 votre compte. Contactez l
Commit: 05c0c0342ededd9b749de5741636b2b6c4fe3c46
File: src/main/resources/WebGoatLabels_fr.properties
Line: 1

**Detector Type: URI**
Raw result: http://guest:guest@127.0.0.1
Commit: 68b80fa14f8c2ece71df20ac0a3e3e710bbcd368
File: webgoat-5.4/src/main/scripts/webgoat.sh
Line: 43

**Detector Type: AWS**
Raw result: AKIAJQLKPGHXRH2AH5QA
Commit: faeb5b1b2486d5b613a55d8730e00b43923683d8
File: .travis.yml
Line: 20

