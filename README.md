# Data Watcher Service.
A policy based flexible data exchange mechanism between two systems.

## Data Watcher Service - A High Level Landscape
![Alt text](data-watcher-service.png?raw=true "High Level Landscape")

## Policy
* A Policy file is a json that's shared between 2 systems that are willing to share data.
* Policy is created by the "Data Requestor" and a copy of that is sent to the "Data Owner".
* Policy defines the rules that the "Data Requestor" wants to apply on the data on the "Data Owner's" data. 
These rules are typically boolean expressions compliant to [JSONATA](https://jsonata.org/) (which is an extremely powerful/flexible/simple json query/transformation library) specification.
* Policy defines the Output data transformation rules that the requestor wants rather than the whole data. The transforation rules are compliant to [JSONATA](https://jsonata.org/)

## An example Policy file:
```
{
    "_id" : "69c4b745-31b9-4b60-925a-5907edca23ae",
    "name" : "Policy 1",
    "description" : "Description 1",
    "version" : 1,
    "originator" : "Originator 1",
    "department" : "Department1",
    "sourceDataType" : "Events",
    "disabled" : false,
    "timestamp" :1122384774,
    "rules" : [ 
        {
            "name" : "Rule1",
            "description" : "Desc1",
            "expression" : "'order103' in Account.Order.OrderID",
            "score" : 10.0
        }, 
        {
            "name" : "Rule2",
            "description" : "Desc1",
            "expression" : "858383 in Account.Order.Product.ProductID",
            "score" : 10.0
        }
    ],
    "threshold" : {
        "lowerBounds" : 5.0,
        "upperBounds" : 100.0
    }
    "outputDefinition" : "{\n'customer': Account.'Account Name',\n\n    'customerPurchaseHistory': {\n    'orderIds': Account.Order.OrderID,\n    'productIds': Account.Order.Product.ProductID\n}\n}",
    "publicKey" : "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4JqALY/yjVBtMlKF1O3UWmV/HEhstkTRpGmtr/1QiTVru3IdA1qdOmVh8xEtYVzvY8t/UXCfVwtJn0wDOHueIZZBQm1iaVyVW+4Tc1RvKgwF4IR3QCnXPVKu1v94mFkKwJnGT2Km/miLxTw5eT/MAXn6yV+E7/cOtql0z2MZTKA5kz+e3yr8lrxSyjE6JWVYlaax2edf5ZCeFDJe34Dwhi2X8e1J82hw2+jsA8UqNgJ3hBCAlR4NIkLky0GZGik9Z5IcglmBoOpufUbucZuPgQ6qDq3HwQJ5T9pv2yGD2rxqVIOg5xLeiI2zP6L8evks97xaTM164Ot5inP0KBty9QIDAQAB",
    "encryptMessage" : false,
}
```