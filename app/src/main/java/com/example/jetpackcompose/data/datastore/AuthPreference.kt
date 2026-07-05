package com.example.jetpackcompose.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.jetpackcompose.data.model.CartItem
import com.example.jetpackcompose.data.remote.dto.RoleDto
import com.example.jetpackcompose.data.remote.dto.UserDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class AuthPreference @Inject constructor(private val context: Context) {

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val PHONE = stringPreferencesKey("phone")
        val AVATAR = stringPreferencesKey("avatar")
        val GENDER = stringPreferencesKey("gender")
        val ROLE = stringPreferencesKey("role")
        val CART_ITEMS = stringPreferencesKey("cart_items")
        val ADDRESS = stringPreferencesKey("address")
        val LATITUDE = stringPreferencesKey("latitude")
        val LONGITUDE = stringPreferencesKey("longitude")
        val LOCATION_POPUP_SHOWN = stringPreferencesKey("location_popup_shown")
    }

    private val gson = Gson()

    suspend fun saveLocation(address: String, lat: Double, lng: Double) {
        context.dataStore.edit { prefs ->
            prefs[ADDRESS] = address
            prefs[LATITUDE] = lat.toString()
            prefs[LONGITUDE] = lng.toString()
        }
    }

    fun getAddress(): Flow<String?> = context.dataStore.data.map { it[ADDRESS] }
    fun getLat(): Flow<Double?> = context.dataStore.data.map { it[LATITUDE]?.toDoubleOrNull() }
    fun getLng(): Flow<Double?> = context.dataStore.data.map { it[LONGITUDE]?.toDoubleOrNull() }

    suspend fun setLocationPopupShown(shown: Boolean) {
        context.dataStore.edit { it[LOCATION_POPUP_SHOWN] = shown.toString() }
    }

    fun isLocationPopupShown(): Flow<Boolean> = context.dataStore.data.map { 
        it[LOCATION_POPUP_SHOWN]?.toBoolean() ?: false 
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            if (refreshToken.isNotEmpty()) {
                prefs[REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun saveUser(user: UserDto) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = user.id ?: ""
            prefs[NAME] = user.name ?: ""
            prefs[EMAIL] = user.email ?: ""
            prefs[PHONE] = user.phone ?: ""
            prefs[AVATAR] = user.avatar ?: ""
            prefs[GENDER] = user.gender ?: ""
            prefs[ROLE] = user.role?.name ?: ""
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { it.clear() }
    }

    fun getAccessToken(): Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    fun getRefreshToken(): Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    fun getUserId(): Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    
    val userData: Flow<UserDto> = context.dataStore.data.map { prefs ->
        UserDto(
            id = prefs[USER_ID],
            name = prefs[NAME],
            email = prefs[EMAIL],
            phone = prefs[PHONE],
            avatar = prefs[AVATAR],
            gender = prefs[GENDER],
            role = RoleDto(id = null, name = prefs[ROLE])
        )
    }

    suspend fun saveCartItems(items: List<CartItem>) {
        context.dataStore.edit { prefs ->
            prefs[CART_ITEMS] = gson.toJson(items)
        }
    }

    fun getCartItems(): Flow<List<CartItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[CART_ITEMS]
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<CartItem>>() {}.type
            gson.fromJson(json, type)
        }
    }
}
