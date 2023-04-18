docker pull ket9/otus-devsecops-xss:latest
docker run -d -p 8080:80 --name otus-05 -e tokentimetolive=6000 ket9/otus-devsecops-xss:latest
