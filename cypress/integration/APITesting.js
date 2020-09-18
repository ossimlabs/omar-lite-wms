  
let json = require('../../testParameters.json');
let tests = Object.keys(json.tests);
let innerJson, method, endpoint, query, parameters, request, keys;

describe('Automated tests for the wms-lite API endpoints', () => {
    tests.forEach((test) => {
        innerJson = json.tests[test];
        method = innerJson["method"];
        endpoint = innerJson["endpoint"];
        query = innerJson["in"] === "query";
        parameters = Object.keys(innerJson["parameters"]);
        if(query) {
            request = "?"
            parameters.forEach((parameter) => {
                request = request + parameter + "=" + innerJson.parameters[parameter] + "&";
            })
            request = request.substring(0, request.length - 1);
            it(`Should test 200 code for ${test} default values`, () => {
                cy.request(method, endpoint + request)
                    .then((response) => {
                        if(expect(response.status).to.eq(200)) {
                            cy.log(`${test} ` + ' request was successfully made with a status of ' + response.status)
                        } else {
                            cy.log(`${test} ` + ' request was not successful status code' + response.status)
                        }
                    })
            })
            it(`Should test response header for ${test}`, () => {
                cy.request(method, endpoint + request)
                    .then((response) => {
                        if(expect(response).to.have.property('headers')) {
                            cy.log(`${test} ` + 'response included header')
                        } else {
                            cy.log(`${test} ` + 'response did not include header')
                        }
                    })
            })
            it(`Should check for header content for ${test}`, () => {
                cy.request(method, endpoint + request)
                    .then((response) => {
                        if(expect(response.headers).to.have.property('content-type').equals('image/jpeg')) {
                            cy.log(`${test} ` + ' response contained content-type:image/jpeg')
                        } else {
                            cy.log(`${test} ` + ' response contained content-type:image/jpeg')
                        }
                    })      
            })

        }
    })
})


