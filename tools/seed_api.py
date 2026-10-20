"""Seed public REST resources; never deletes or replaces existing records.
Usage: python tools/seed_api.py [https://PROJECT.mockapi.io]
"""
import json
from pathlib import Path
import sys
import urllib.request
import urllib.error

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'https://6ad6ffc2f71f154ac4879555.mockapi.io').rstrip('/')
def request(method, path, body=None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(BASE + path, data=data, method=method,
        headers={'Content-Type': 'application/json', 'Accept': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        raise RuntimeError(f'{method} {path}: HTTP {e.code}. Check Base URL and resources.') from e

def main():
    sample = json.loads((Path(__file__).resolve().parents[1] / 'app/src/main/assets/sample-data.json').read_text(encoding='utf-8'))
    categories, courses = request('GET', '/categories'), request('GET', '/courses')
    if not isinstance(categories, list) or not isinstance(courses, list):
        raise RuntimeError('Expected arrays from both resources.')
    mapping = {}
    for c in sample['categories']:
        found = next((a for a in categories if a.get('name') == c['name']), None)
        if found is None:
            found = request('POST', '/categories', {k: c[k] for k in ['name', 'description']})
            print('Created category:', found['id'], c['name'])
        mapping[c['key']] = str(found['id'])
    for c in sample['courses']:
        if any(a.get('title') == c['title'] for a in courses):
            continue
        body = {k: v for k, v in c.items() if k != 'categoryKey'}
        body['categoryId'] = mapping[c['categoryKey']]
        found = request('POST', '/courses', body)
        # Check that the backend preserved every required field, not just the id.
        for key, value in body.items():
            if str(found.get(key)) != str(value):
                raise RuntimeError(f'MockAPI did not preserve {key}; add that field to the resource schema.')
        print('Created course:', found['id'], c['title'])
    categories, courses = request('GET', '/categories'), request('GET', '/courses')
    ids = {str(c['id']) for c in categories}
    assert len(categories) >= 3 and len(courses) >= 10
    assert all(str(c.get('categoryId')) in ids for c in courses), 'Found an orphan categoryId.'
    print(f'OK: {len(categories)} categories, {len(courses)} courses; all categoryId references exist.')

if __name__ == '__main__':
    main()
