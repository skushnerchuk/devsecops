## Безопасная разработка в Python

### I. Исследование кода на Python

**Фрагмент 1**

```python
from flask import Flask
from mod_api import mod_api

app = Flask('vuln_app')
app.config['SECRET_KEY'] = 'F0cUzh8BgYJSLXAU8qDmClM0dE8GJTpsiyVEl3BCqQMCABp1U$f%'
app.register_blueprint(mod_api, url_prefix='/api')
```

Убрать значение SECRET_KEY из кода и передавать его через переменную окружения:

```
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY')
```

Заменить `app = Flask('vuln_app')` на `app = Flask(__name__)`, чтобы приложение правильно понимало, откуда оно запущено и смогло корректно выполнить настройки.

Найденных проблем: 2

**Фрагмент 2**

```python
from flask_wtf import Form
from wtforms import TextField


class LoginForm(Form):
    username = TextField('username')
    password = TextField('password')
```

В модуле **wtforms** отсутствует класс **TextField**, поэтому его надо заменить на другие.

Также некорректно использовать для ввода пароля обычное текстовое поле, и необходимо объявить оба поля обязательными.

Изменим класс **LoginForm** следующим образом:

```python
from flask_wtf import Form
from wtforms import StringField, PasswordField
from wtforms.validators import InputRequired


class LoginForm(Form):
    username = StringField('username', validators=[InputRequired()])
    password = PasswordField('password', validators=[InputRequired()])
```

Найденных проблем: 3

**Фрагмент 3**

```python
def post(self):
    username = self.get_argument('username')
    password = self.get_argument('password').encode('utf-8')
    email = self.get_argument('mail')
    try:
        username = username.lower()
        email = email.strip().lower()
        user = User({'username': username, 'password': password, 'email': email, 'date_joined': curtime()})
        user.validate()
        save_user(self.db_conn, user)
    except Exception, e:
        return self.render_template("success_create.html")
```

Разберем код построчно.

```python
password = self.get_argument('password').encode('utf-8')
```

**get_argument** может вернуть значение **None**, и это приведет к тому, что программа выбросит исключение:

```python
AttributeError: 'NoneType' object has no attribute 'encode'
```

вследствие чего возникшее исключение не будет обработано внутри функции, и это может привести к отображению всей информации об исключении пользователю, если в приложении не предусмотрен единый обработчик для всех неперехваченных исключений.

```python
username = username.lower()
```

Значение переменной user получено вызовом функции **self.get_argument**, которая может возвращать **None**. Это может привести к исключению

```python
AttributeError: 'NoneType' object has no attribute 'lower'
```

Также здесь явно не хватает вызова функции strip(), которая обрезала бы пробельные символы вокруг имени пользователя, ведь если случайно после имени пользователь нажал пробел, и не заметил этого, то потом не сможет авторизоваться, если для входа требуется логин, а не адрес электронной почты. 

```
email = email.strip().lower()
```

Эта строка подвержена проблемам, аналогичным упомянутым выше, но так же приведение адреса электронной почты к нижнему регистру может вызвать проблемы на некоторых почтовых хостингах.

В соответствии с [RFC-5321](https://www.rfc-editor.org/rfc/rfc5321#section-2.3.11) семантика локальной части (до символа @) определяется и интерпретируется исключительно хостом, тогда как доменная часть (после символа @) должна сравниваться без учета регистра в соответствии с [RFC-1035](https://www.rfc-editor.org/rfc/rfc1035). То есть может случится так, что пользователь никогда не получит писем от нашего сервиса.

```python
user = User({'username': username, 'password': password, 'email': email, 'date_joined': curtime()})
user.validate()
save_user(self.db_conn, user)
```

Здесь неочевидный момент. Мы предположительно видим, что пароль пользователя будет сохранен в открытом виде (причем не как строка, а как последовательность байт, из-за вызова функции encode), и его стоит заменить на хэш с использованием стойких алгоритмов хеширования и добавлением соли.

Однако, после создания объекта вызывается функция **validate**, которая должна проверить корректность данных пользователя. Предположим, что эта функция предъявляет некие требования к паролю. Тогда, если пароль не был указан (пустая строка), а мы выполнили его хеширование **до** проверки, то его значение, переданное в объект, не будет пустым (например, хэш пустой строки в sha256 стандартного модуля python hashlib может иметь значение e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855).

При этом остается странным решением вызов функции **encode** при получении значения пароля - это усложняет его валидацию, и скорее всего приведет к исключению при сохранении данных в базе, так как пароль станет уже не строковым, а байтовой последовательностью (если сохранение пароля предполагалось в открытом виде). 

Логичным вариантом является преобразование пароля в хэш после валидации данных, но перед сохранением в базе.

```
except Exception, e:
```

Эта строка содержит синтаксическую ошибку. Корректно строка должна выглядеть так:

```
except Exception as e:
```

При этом тут есть еще несколько проблем.

Переменная **e** не используется в обработчике, поэтому может быть безболезненно убрана: `except Exception:`

Перехват базового исключения Exception приведет к тому, что вне зависимости от того, в каком месте возникла ошибка внутри блока **try/except** и что являлось ее причиной, будет выведено некое сообщение пользователю. (кстати тоже неверное, если исходить из названия шаблона, который должен сообщить о успешной регистрации).  Поэтому перехватывать в этом блоке необходимо только те типы исключений, которые ожидаются, а все что будет пропущено должно быть обработано в единой точке обработки не перехваченных исключений.

Также необходимо экранировать все данные, которые мы получили от пользователя (условно - функция **escape**), и уведомлять пользователя о том, что регистрация прошла успешно, выведя сообщение или выполнив редирект в какую-то защищенную часть приложения (если не требуется подтверждения аккаунта через электронную почту).

Исправленный код будет выглядеть примерно так:

```python
def post(self):
    username = self.get_argument('username') or ''
    password = self.get_argument('password') or ''
    email = self.get_argument('mail') or ''
    try:
        username = escape(username.strip().lower())
        email = escape(email.strip())
        user = User({'username': username, 'password': password, 'email': email, 'date_joined': curtime()})
        user.validate()
        user.password = strong_hash(password)
        save_user(self.db_conn, user)
        return self.render_template("success_create.html")
    except (DBException, UserValidateException):
        return self.render_template("fail_create.html")
```

При отображении ошибки пользователю также разумно было бы показать причину, по которой регистрация не выполнена (не указан пароль, неверный формат адреса электронной почты и т.п.).

Найденных проблем: 9

### II. Модель безопасности в Django

Последняя версия Django (на момент написания - 4.2) имеет [встроенные механизмы](https://docs.djangoproject.com/en/4.2/topics/security/) для защиты от ряда атак.

**Cross site scripting (XSS)**

Атаки XSS позволяют пользователю внедрять клиентские скрипты в браузеры других пользователей. Обычно это достигается путем хранения вредоносных скриптов в базе данных, где они будут извлекаться и отображаться другим пользователям, или путем побуждения пользователей нажимать на ссылку, которая приведет к запуску JavaScript злоумышленника в браузере пользователя. 

Использование шаблонов Django защищает большинства XSS-атак. Однако важно понимать, какие средства защиты он предоставляет и его ограничения.

Шаблоны Django [экранируют определенные символы](https://docs.djangoproject.com/en/4.2/ref/templates/language/#automatic-html-escaping), которые особенно опасны для HTML. Хотя это защищает от большинства вредоносных данных, это не совсем надежно. Например, это не защитит следующее:

```
<style class={{ var }}>...</style>
```

Если `var` установлено значение `'class1 onmouseover=javascript:func()'`, это может привести к несанкционированному выполнению JavaScript, в зависимости от того, как браузер отображает несовершенный HTML.

Кроме того, если используется система шаблонов для вывода чего-либо, отличного от HTML, могут быть совершенно другие символы и слова, которые требуют экранирования.

**Cross site request forgery (CSRF)**

CSRF-атаки позволяют злоумышленнику выполнять действия, используя учетные данные другого пользователя без его ведома.

Django имеет [встроенную защиту](https://docs.djangoproject.com/en/4.2/howto/csrf/#using-csrf) от большинства типов CSRF-атак, однако существует ряд ограничений. Например, можно отключить модуль CSRF глобально или для определенных представлений. Существуют и другие [ограничения](https://docs.djangoproject.com/en/4.2/ref/csrf/#csrf-limitations), если у приложения есть поддомены, которые находятся вне  контроля.

[Защита CSRF работает](https://docs.djangoproject.com/en/4.2/ref/csrf/#how-csrf-works) путем проверки наличия секрета в каждом POST-запросе. Это гарантирует, что злоумышленник не сможет выполнить отправку формы в приложение, ведь он должен был бы знать секрет, который зависит от конкретного пользователя.

**SQL injection**

SQL-инъекция - это тип атаки, при котором злоумышленник может выполнить произвольный SQL-код в базе данных. 

Django защищен от таких инъекций, поскольку запросы создаются с использованием параметризации. Так как параметры могут предоставляться пользователем и, следовательно, небезопасны, они экранируются базовым драйвером базы данных.

Django также дает разработчикам возможность писать [необработанные запросы](https://docs.djangoproject.com/en/4.2/topics/db/sql/#executing-raw-queries) или выполнять [пользовательский sql](https://docs.djangoproject.com/en/4.2/topics/db/sql/#executing-custom-sql), однако эти возможности следует использовать осторожно с полным пониманием того, что делается.

**Clickjacking**

Clickjacking - это тип атаки, при котором вредоносный сайт помещает другой сайт во фрейм. Эта атака может привести к тому, что ничего не подозревающий пользователь будет обманом принужден к выполнению каких-либо действий на целевом сайте.

Django содержит вариант [защиты](https://docs.djangoproject.com/en/4.2/ref/clickjacking/#clickjacking-prevention) в middleware [X-Frame-Options](https://docs.djangoproject.com/en/4.2/ref/middleware/#django.middleware.clickjacking.XFrameOptionsMiddleware), которая может препятствовать отображению сайта внутри фрейма. Можно отключить защиту для каждого просмотра или настроить точное отправляемое значение заголовка.

X-Frame-Options настоятельно рекомендуется для любого приложения, которому не требуется отображение сторонних ресурсов.

**SSL/HTTPS**

Для обеспечения безопасности всегда лучше использовать протокол HTTPS. Без этого возможен перехват учетных данных или любуой другуой информации, передаваемой между клиентом и сервером, а в некоторых случаях возможна и их подделка.

Для активации поддержки HTTPS в приложении Django необходимо выполнить ряд шагов.

- При необходимости установите [SECURE_PROXY_SSL_HEADER](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SECURE_PROXY_SSL_HEADER), предварительно прочитав предупреждения, указанные в документации. Невыполнение этого требования может привести к уязвимостям CSRF.

- Установите [SECURE_SSL_REDIRECT](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SECURE_SSL_REDIRECT) в `True`, чтобы запросы по HTTP перенаправлялись на HTTPS.

- Используйте безопасные cookie.

  Если браузер изначально подключается через HTTP, который используется по умолчанию для большинства браузеров, возможна утечка существующих файлов cookie. По этой причине следует установить [SESSION_COOKIE_SECURE](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SESSION_COOKIE_SECURE) и [CSRF_COOKIE_SECURE](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-CSRF_COOKIE_SECURE) настройки в **True**. Это предписывает браузеру отправлять сookie только по HTTPS-соединениям. Это также будет означать, что сеансы не будут работать через HTTP, а защита CSRF предотвратит прием любых данных POST через HTTP (что будет нормально, если вы перенаправляете весь HTTP-трафик на HTTPS).

- Используйте [HTTP Strict Transport Security](https://docs.djangoproject.com/en/4.2/ref/middleware/#http-strict-transport-security) (HSTS)

  HSTS - это HTTP-заголовок, который информирует браузер о том, что все будущие подключения к сайту всегда должны использовать HTTPS. В сочетании с перенаправлением запросов через HTTP на HTTPS это гарантирует, что соединения всегда будут пользоваться дополнительной безопасностью SSL при условии, что произошло одно успешное соединение. HSTS могут быть настроены либо с помощью переменных [SECURE_HSTS_SECONDS](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SECURE_HSTS_SECONDS), [SECURE_HSTS_INCLUDE_SUBDOMAINS](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SECURE_HSTS_INCLUDE_SUBDOMAINS) и [SECURE_HSTS_PRELOAD](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SECURE_HSTS_PRELOAD), либо на веб-сервере.

**Host header validation**

В некоторых случаях Django использует заголовок **Host**, предоставленный клиентом, для создания URL-адресов. 

Поскольку даже кажущиеся безопасными конфигурации веб-сервера подвержены поддельным заголовкам Host, Django проверяет его на соответствие настройке [ALLOWED_HOSTS](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-ALLOWED_HOSTS).

Кроме того, Django требует, явно включенной поддержки заголовка **X-Forwarded-Host** (через [USE_X_FORWARDED_HOST](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-USE_X_FORWARDED_HOST)), если этого требует ваша конфигурация.

**Referrer policy**

Браузеры используют заголовок **Referer** как способ отправки информации о том, как пользователи попали на него. Устанавливая политику ссылок, вы можете помочь защитить конфиденциальность ваших пользователей, ограничивая, при каких обстоятельствах устанавливается этот заголовок.

**Cross-origin opener policy**

Заголовок **Cross-Origin-Opener-Policy** позволяет браузерам изолировать окно верхнего уровня от других документов, помещая их в другую контекстную группу, чтобы они не могли напрямую взаимодействовать с окном верхнего уровня. Если документ, защищенный COOP, открывает всплывающее окно с разными источниками, свойство `window.opener` во всплывающем окне будет **null**.

**Пользовательские загрузки**

Если приложение позволяет выполнять загрузку файлов, настоятельно рекомендуется ограничить размер загружаемых файлов в конфигурации веб-сервера, чтобы предотвратить атаки типа "отказ в обслуживании"

Если предполагается обслуживание собственных статических файлов, обработчики, подобные **Apache mod_php**, которые выполняли бы статические файлы в виде кода, стоит отключить.

Обработка загрузки мультимедиа в Django создает некоторые уязвимости, когда этот носитель обслуживается способами, которые не соответствуют рекомендациям по безопасности. В частности, HTML-файл может быть загружен в виде изображения, если этот файл содержит допустимый заголовок PNG, за которым следует вредоносный HTML. Этот файл пройдет проверку библиотекой, которую Django использует для обработки изображений (Pillow). Когда этот файл впоследствии отображается пользователю, он может отображаться как HTML в зависимости от типа и конфигурации веб-сервера.

На уровне фреймворка не существует единого технического решения для безопасной проверки всего содержимого загруженных пользователем файлов, однако могут быть предприняты некоторые шаги для смягчения последствий этих атак:

1. Один класс атак можно предотвратить, всегда обслуживая загруженный пользователем контент из отдельного домена верхнего уровня или второго уровня. Это предотвращает любой эксплойт, заблокированный с помощью [same origin policy](https://en.wikipedia.org/wiki/Same-origin_policy), такой как межсайтовый скриптинг.
2. Приложения могут определять список допустимых расширений файлов для загруженных пользователем файлов и настраивать веб-сервер для обслуживания только таких файлов.

#### Пользовательские сессии в Django

Django предоставляет полную поддержку пользовательских сеансов. Встроенная реализация сеансов позволяет хранить и извлекать данные для каждого пользователя, при этом в передаваемом cookie содержится только идентификатор сессии, а не сами данные (если не используются сессии на [основе cookie](https://docs.djangoproject.com/en/4.2/topics/http/sessions/#cookie-session-backend)).

**Добавление поддержки сеансов в приложение**

Сеансы реализуются с помощью [middleware](https://docs.djangoproject.com/en/4.2/ref/middleware/).

Чтобы включить функциональность сеанса, необходимо отредактировать переменную [MIDDLEWARE](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-MIDDLEWARE), добавив значение `django.contrib.sessions.middleware.SessionMiddleware`.

**Настройка механизма сеансов**

По умолчанию Django хранит сеансы в базе данных (используя модель `django.contrib.sessions.models.Session`). Хотя это удобно, в некоторых случаях лучше хранить данные сеанса в другом месте, поэтому Django можно настроить для хранения данных сеанса в файловой системе или в кэше.

**База данных**

Для использования сессии с поддержкой базы данных, необходимо добавить `'django.contrib.sessions'` в переменную [INSTALLED_APPS](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-INSTALLED_APPS). После этого нужно выполнить команду `manage.py migrate` для добавления необходимых таблиц в базу данных.

**Сессии на основе кэша**

Использование сессий на основе кэша позволяет повысить производительность приложения.

Чтобы сохранить данные сеанса с использованием системы кэширования Django, нужно убедиться, что [кэш правильно настроен](https://docs.djangoproject.com/en/4.2/topics/cache/).

Сеансы на основе кэша следует применять только в том случае, если планируется использовать Memcached или Redis. Кэш в оперативной памяти недолговечен и доступен для разных процессов, что не является безопасным.

**Сессии на основе файлов**

Чтобы использовать сессии на основе файлов, в переменной [SESSION_ENGINE](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SESSION_ENGINE) нужно установить значение `"django.contrib.sessions.backends.file"`.

Также в переменной [SESSION_FILE_PATH](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SESSION_FILE_PATH) можно установить путь к директории, где будут хранится файлы сессии. Соответственно, веб-сервер должен иметь разрешения на чтение и запись в эту директорию.

**Сессии на основе cookie**

Чтобы использовать сессии на основе cookie,в переменной [SESSION_ENGINE](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SESSION_ENGINE) нужно установить значение `"django.contrib.sessions.backends.signed_cookies"`. Данные сеанса будут сохранены с учетом [криптографической подписи](https://docs.djangoproject.com/en/4.2/topics/signing/) и значения переменной [SECRET_KEY](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SECRET_KEY).

Рекомендуется оставить переменную [SESSION_COOKIE_HTTPONLY](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SESSION_COOKIE_HTTPONLY) в значении **True**, чтобы предотвратить доступ к сохраненным данным из JavaScript.

Сессии в Django могут быть сериализованы. Не рекомендуется выполнять сериализацию сеансов с использованием класса **PickleSerializer**, так как он не является безопасным и его использование может привести к удаленному выполнению кода злоумышленником, если значение переменной **SECRET_KEY** скомпрометировано. По умолчанию используется сериализатор **JSONSerializer**, однако он не может обрабатывать произвольные данные. Если есть необходимость хранить в сессии данные, не совместимые с форматом **JSON**, то Django предоставляет возможность реализовать свой сериализатор.

По умолчанию Django сохраняет данные сессии в базе данных только тогда, когда сессия была изменена (добавлено или удалено поле, или изменено значение какого-либо поля). Чтобы сохранять значение сессии при каждом запросе, то необходимо установить значение перменной [SESSION_SAVE_EVERY_REQUEST](https://docs.djangoproject.com/en/4.2/ref/settings/#std-setting-SESSION_SAVE_EVERY_REQUEST)  в True.

**Очистка хранилища сессий**

По мере создания новых сессий в приложении, данные будут накапливаться в хранилище сессий. Если для хранения используется база данных, таблица `django_session` будет расти. Если используются файлы, то каталог, где хранятся файлы сессий, будет содержать все большее количество файлов.

Django *не* обеспечивает автоматическую очистку сессий с истекшим сроком действия, следовательно, задача регулярного удаления таких сессий перекладывается на разработчика. Django предоставляет для этого команду [clearsessions](https://docs.djangoproject.com/en/4.2/ref/django-admin/#django-admin-clearsessions). Рекомендуется вызывать эту команду на регулярной основе, например, в качестве ежедневного задания cron.

В случае использования кэша эта проблема не возникает, так как устаревшие данные удялются автоматически. При использовании cookie этой проблемы тоже нет, так как они хранятся на стороне пользователей.

Также Django предоставляет механизмы для собственной реализации сессий на основе существующих.