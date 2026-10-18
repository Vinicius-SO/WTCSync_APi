"""One-off production support probe; cleans up only its uniquely named fixture."""
import json, secrets, uuid, urllib.request, urllib.error
from pathlib import Path
from pymongo import MongoClient

BASE = 'https://wtcsync-api.onrender.com'
email = 'wtcsync-support-probe-' + uuid.uuid4().hex + '@example.invalid'
password = secrets.token_urlsafe(32)
props = dict(line.split('=', 1) for line in Path('src/main/resources/application.properties').read_text().splitlines()
             if '=' in line and not line.lstrip().startswith('#'))
mongo = MongoClient(props['spring.data.mongodb.uri'].strip(), serverSelectionTimeoutMS=15000)
# Verify cleanup access before creating anything through the API.
mongo.admin.command('ping')
databases = {'test'}
default = mongo.get_default_database(default='test').name
databases.add(default)
for db in databases:
    assert mongo[db].users.find_one({'email': email}) is None

def req(path, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(BASE + path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=150) as r:
            return r.status, json.loads(r.read() or b'{}')
    except urllib.error.HTTPError as e:
        return e.code, {}

try:
    status, _ = req('/api/auth/register', {'email': email, 'password': password, 'name': 'Temporary support diagnostic'})
    print('REGISTER_STATUS', status, flush=True)
    if status == 200:
        status, body = req('/api/auth/login', {'email': email, 'password': password})
        print('LOGIN_STATUS', status, flush=True)
        token = body.get('token')
        if token:
            status, body = req('/api/auth/me', token=token)
            print('PROFILE_STATUS', status, 'ROLE', body.get('role'), flush=True)
            status, body = req('/api/clients/me', token=token)
            print('SUPPORT_STATUS', status, 'HAS_ID', bool(body.get('id')), flush=True)
            if status == 200 and body.get('id'):
                status, _ = req('/inbox/' + body['id'], token=token)
                print('INBOX_STATUS', status, flush=True)
            for db in databases:
                if mongo[db].users.find_one({'email': email}):
                    doc = mongo[db].clients.find_one({'email': email})
                    print('SUPPORT_RECORD_PRESENT', doc is not None, flush=True)
                    print('CLIENT_COLLECTION_VALIDATOR', mongo[db].clients.options().get('validator', {}), flush=True)
finally:
    for db in databases:
        print('CLEANUP_CLIENTS', mongo[db].clients.delete_many({'email': email}).deleted_count, flush=True)
        print('CLEANUP_USERS', mongo[db].users.delete_many({'email': email}).deleted_count, flush=True)
    mongo.close()
