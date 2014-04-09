package com.myhoard.app.model;

/*
{
  id = string
  name = string
  description = string
  location = {
    lat = float
    lng = float
  }
  media = [
    {
        "id": "c853ef896cd5093c6e455dbdc1fc2ec0",
        "url": "http://myhoard.host/media/c853ef896cd5093c6e455dbdc1fc2ec0"
    },
    {
        "id": "c853ef896cd5093c6e455dbdc1dhj3sd",
        "ur": "http://myhoard.host/media/c853ef896cd5093c6e455dbdc1dhj3sd"
    },
    {
        "id": "c853ef896cd5093c6e455dbdc1332222"
        "http://myhoard.host/media/c853ef896cd5093c6e455dbdc1332222"
    }
  ]
  created_date = date
  modified_date = date
  collection = string  # fk_collection_id
  owner = string  # fk_user_username
}
 */


import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Description
 *
 * @author Marcin Łaszcz
 *         Date: 02.04.14
 */
public class Item implements IModel
{
    public String id;
    public String name;
    public String description;
    public ItemLocation location;
    public List<ItemMedia> media = new ArrayList<>();
    @SerializedName("created_date")
    public String createdDate;
    @SerializedName("modified_date")
    public String modifiedDate;
    public String collection;

    public Item(String id, String name, String description, ItemLocation location, List<ItemMedia> media,
                String createdDate, String modifiedDate, String collection) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.media = media;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.collection = collection;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        final String API_ID = "id";
        final String API_NAME = "name";
        final String API_DESC = "description";
        final String API_LOCATION = "location";
        final String API_MEDIA = "media";
        final String API_CREATED_DATE = "created_date";
        final String API_MOD_DATE = "modified_date";
        final String API_COLLECTION = "collection";

        JSONObject json = new JSONObject();

        json.put(API_ID, id);
        json.put(API_NAME, name);
        json.put(API_DESC, description);

        if (location != null) {
            json.put(API_LOCATION, location.toJson());
        }

        if (media != null) {
            JSONArray arrayMedia = new JSONArray();
            for (ItemMedia m : media) {
                arrayMedia.put(m.id);
            }
            json.put(API_MEDIA, arrayMedia);
        }

        //json.put(API_CREATED_DATE, createdDate);
        //json.put(API_MOD_DATE, modifiedDate);
        json.put(API_COLLECTION, collection);

        return json;
    }
}


