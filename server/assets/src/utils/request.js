import request from "superagent";
import nocache from "src/utils/request-no-cache";

function toPromise(request) {
  return new Promise((resolve, reject) => {
    request.end((err, res) => {
      if (err) {
        err.response = res;
        reject(err);
      } else {
        resolve(res.body);
      }
    });
  });
}

function get(uri, queries, cache) {
  const req = request.get(uri);
  if (!cache) {
    req.use(nocache)
  }
  if (queries) {
    req.query(queries);
  }
  req.set("Accept", "application/json");
  return toPromise(req);
}

function del(uri, queries) {
  const req = request.del(uri);
  if (queries) {
    req.query(queries);
  }
  return toPromise(req);
}

function post(uri, json, queries) {
  const req = request.post(uri).use(nocache);
  if (queries) {
    req.query(queries);
  }
  if (json) {
    req
      .set("Content-Type", "application/json")
      .send(json);
  }
  req.set("Accept", "application/json");
  return toPromise(req);
}

function put(uri, json, queries) {
  const req = request.put(uri).use(nocache);
  if (queries) {
    req.query(queries);
  }
  req
    .set('Content-Type', 'application/json')
    .send(json)
    .set("Accept", "application/json");
  return toPromise(req);
}

export default {
  json: {
    getPlaces: () => {
      return get("/assets/jsons/place.json", null, true);
    }
  },
  admiral: {
    create: () => {
      return post("/admirals");
    },
    update: (admiral, name) => {
      return put(`/admirals/${admiral.admiralId}`, { name }, { created_at: admiral.created });
    }
  },
  anchor: {
    list: (prefecture, place, credits) => {
      return get(`/anchors/${prefecture}/${place}/${credits}`);
    },
    create: (anchor, admiral) => {
      return post("/anchors", anchor, { created_at: admiral.created });
    },
    updatePosition: (anchor, page, number, admiral) => {
      return put(`/anchors/${anchor.prefecture}/${anchor.place}/${anchor.credits}/${anchor.admiral.admiralId}/${anchor.anchored}`, {
        page,
        number
      }, {
        created_at: admiral.created
      });
    },
    updateWeighed: (anchor, weighed, admiral) => {
      return put(`/anchors/${anchor.prefecture}/${anchor.place}/${anchor.credits}/${anchor.admiral.admiralId}/${anchor.anchored}`, {
        weighed
      }, {
        created_at: admiral.created
      });
    },
    del: (anchor, admiral) => {
      return del(`/anchors/${anchor.prefecture}/${anchor.place}/${anchor.credits}/${anchor.admiral.admiralId}/${anchor.anchored}`, {
        created_at: admiral.created
      });
    }
  },
  spotting: {
    list: (prefecture, place, admiral) => {
      return get(`/spottings/${prefecture}/${place}`, {
        uid: admiral.admiralId,
        created_at: admiral.created
      });
    },
    create: (spotting, admiral) => {
      return post(`/spottings`, spotting, { created_at: admiral.created });
    }
  }
}
