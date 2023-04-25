docker pull ket9/otus-devsecops-owasp-rest
docker run -d -p 8080:8080 --name otus-05 ket9/otus-devsecops-owasp-rest:latest
while ! nc -z localhost 8080; do
  sleep 1.0
done
sleep 1.0
curl http://localhost:8080/createdb
