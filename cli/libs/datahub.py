#!/usr/bin/env python
import os
import requests
import json

__author__ = 'Andrea Rossi'
__description = '''\
    DataHub is a web application for easily organising translation resources.
    It is developed within the project ModernMT and is specifically tailored
    with the needs of translation resources management in mind.
    DataHub main features include:
        - storing resources;
        - searching for resources;
        - downloading resources;
        - resource statistics;
    These operations can be performed either from the application GUI or through REST APIs.

    For more details, visit our github page: 'https://github.com/ModernMT/DataHub'
'''


#
class DataHub:
    def __init__(self, host='datahub.rocks.mmt', port=80):
        self.api = Api(host, port)
        self.auth = None

    # ================ API CALLING METHODS ================ #

    def authenticate(self, username=None, password=None):
        self.auth = self.api.login(username, password)['credentials']
        return self.auth

    def validate_auth(self, access_token):
        if self.api.check_auth(access_token):
            self.auth = access_token
        return self.auth

    def generate_collection(self, source, target, source_words, target_words):
        return self.api.generate_collection(source, target, source_words, target_words, self.auth)

    def download_collection(self, collection, destination_folder):
        return self.api.download_collection(str(collection["id"]), collection["name"], destination_folder, self.auth)


# ================ SCRIPT-AVAILABLE API ================ #
class Api:
    def __init__(self, host, port):
        self._api_url = 'http://' + os.path.join(host + ":" + str(port), "api")

    def login(self, username, password):
        url = os.path.join(self._api_url, "login")
        data = {'username': username, 'password': password}
        response = requests.post(url=url, data=data)
        if response.status_code != 200:
            raise Exception("Authentication error: %s" % response.content)
        return json.loads(response.text)

    def check_auth(self, access_token):
        url = os.path.join(self._api_url, "users", "me")
        response = requests.get(url=url, headers={"Authorization": access_token})
        if response.status_code != 200:
            raise Exception("Authentication error: %s" % response.content)
        return True

    def generate_collection(self, source, target, source_words, target_words, auth):
        url = os.path.join(self._api_url, "collection")
        data = {'source': source, 'target': target, 'sourceWords': source_words, 'targetWords': target_words}
        response = requests.get(url=url, params=data, headers={"Authorization": auth})
        return json.loads(response.text)

    def download_collection(self, collection_id, collection_name, destination_folder, auth):
        url = os.path.join(self._api_url, "collection", collection_id, "corpora")
        local_filepath = os.path.join(destination_folder, "datahub_collection_" + collection_name + ".zip")
        response = requests.get(url=url, headers={"Authorization": auth}, stream=True)
        with open(local_filepath, 'wb') as f:
            for chunk in response.iter_content(chunk_size=1024):
                if chunk:
                    f.write(chunk)
        return response.status_code
