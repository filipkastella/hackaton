import Mapbox from "@rnmapbox/maps";
import * as Location from "expo-location";
import React, { useEffect, useRef, useState } from "react";
import {
  Animated,
  Dimensions,
  Easing,
  Keyboard,
  PanResponder,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View
} from "react-native";

// Mapbox API access token
const MAPBOX_TOKEN =
  "sk.eyJ1IjoiYXBrYXN2IiwiYSI6ImNtaHBkcXYyMjBqNHMyaXBnZzUzaDF1NmsifQ.YerdtQnb2UN4CEZOFqxxQA";

Mapbox.setAccessToken(MAPBOX_TOKEN);

const { height } = Dimensions.get("window");
const COLLAPSED_HEIGHT = 80;
const EXPANDED_HEIGHT = height * 0.82;


export default function HomeScreen() {
  const [userCoords, setUserCoords] = useState<number[] | null>(null);
  const [destination, setDestination] = useState<number[] | null>(null);
  const [destinationName, setDestinationName] = useState("");
  const [suggestions, setSuggestions] = useState<any[]>([]);
  const [routeType, setRouteType] = useState<"driving" | "walking">("driving");
  const [routeCoords, setRouteCoords] = useState<number[][] | null>(null);
  const [eta, setEta] = useState<string | null>(null);
  const [distance, setDistance] = useState<string | null>(null);
  const [nextStep, setNextStep] = useState<string | null>(null);


  const translateY = useRef(new Animated.Value(height - COLLAPSED_HEIGHT)).current;
  const [isExpanded, setIsExpanded] = useState(false);
  const cameraRef = useRef<Mapbox.Camera>(null);
  const opacityAnim = useRef(new Animated.Value(0)).current;


  interface User {
    userName: string;
    id: string;
    groupId?: string;
  }

  const [user, setUser] = useState<User | null>(null);


  // === Get user location and track changes ===
  useEffect(() => {
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") {
        alert("Permission to access location was denied");
        return;
      }
    // try to catch actual geo 
    let lastKnown = await Location.getLastKnownPositionAsync({});
    if (lastKnown) {
      setUserCoords([lastKnown.coords.longitude, lastKnown.coords.latitude]);
    }

    // if not - request again
    const current = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.High,
    });
    setUserCoords([current.coords.longitude, current.coords.latitude]);

    // after sucsess - folowing actual geolocation data
    await Location.watchPositionAsync(
      {
        accuracy: Location.Accuracy.High,
        distanceInterval: 5,
      },
      (loc) => {
        setUserCoords([loc.coords.longitude, loc.coords.latitude]);
      }
    );

    })();
  }, []);

  const centerCamera = () => {
    if (!userCoords) return;
    cameraRef.current?.setCamera({
      centerCoordinate: userCoords,
      zoomLevel: 15,
      animationDuration: 1000,
    });
  };

  const makeGroupButton = () => {
    console.log("makeGroupButton");
  };

  const addToGroupButton = () => {
    console.log("addToGroupButton");
  };


  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (_, g) => Math.abs(g.dy) > 5,
      onPanResponderMove: (_, g) => {
        let newY = isExpanded
          ? height - EXPANDED_HEIGHT + g.dy
          : height - COLLAPSED_HEIGHT + g.dy;

        translateY.setValue(
          Math.min(height - COLLAPSED_HEIGHT, Math.max(height - EXPANDED_HEIGHT, newY))
        );
      },
      onPanResponderRelease: (_, g) => {
        if (g.dy < -50) openMenu();
        else if (g.dy > 50) closeMenu();
        else isExpanded ? openMenu() : closeMenu();
      },
    })
  ).current;

  const openMenu = () => {
    setIsExpanded(true);
    Animated.timing(translateY, {
      toValue: height - EXPANDED_HEIGHT,
      duration: 300,
      easing: Easing.out(Easing.exp),
      useNativeDriver: true,
    }).start();
  };

  const closeMenu = () => {
    setIsExpanded(false);
    Animated.timing(translateY, {
      toValue: height - COLLAPSED_HEIGHT,
      duration: 300,
      easing: Easing.inOut(Easing.exp),
      useNativeDriver: true,
    }).start();
  };

  const handlePress = () => (isExpanded ? closeMenu() : openMenu());

  useEffect(() => {
    const delay = setTimeout(async () => {
      if (destinationName.length < 3) return setSuggestions([]);

      try {
        const resp = await fetch(
          `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(
            destinationName
          )}.json?autocomplete=true&limit=5&access_token=${MAPBOX_TOKEN}`
        );
        const data = await resp.json();
        setSuggestions(data.features || []);
      } catch (err) {
        console.error("Autocomplete error", err);
      }
    }, 400);
    return () => clearTimeout(delay);
  }, [destinationName]);

  const handleSelectSuggestion = (item: any, e?: any) => {
    e?.stopPropagation?.();
    const coords = item.center;
    setDestination(coords);
    setDestinationName(item.place_name);
    setSuggestions([]);
  };

  const buildRoute = async () => {
    if (!userCoords || !destination) return alert("Missing coordinates");

    try {
      const [startLng, startLat] = userCoords;
      const [destLng, destLat] = destination;

      const url = `https://api.mapbox.com/directions/v5/mapbox/${routeType}/${startLng},${startLat};${destLng},${destLat}?geometries=geojson&steps=true&overview=full&access_token=${MAPBOX_TOKEN}`;
      const res = await fetch(url);
      const data = await res.json();

      if (!data.routes?.[0]) throw new Error("No route found");

      const route = data.routes[0];
      setRouteCoords(route.geometry.coordinates);
      setDistance((route.distance / 1000).toFixed(1) + " km");
      setEta(Math.round(route.duration / 60) + " min");

      const firstStep = route.legs?.[0]?.steps?.[0]?.maneuver?.instruction;
      if (firstStep) setNextStep(firstStep);

      Animated.timing(opacityAnim, {
        toValue: 1,
        duration: 600,
        useNativeDriver: true,
      }).start();

      cameraRef.current?.fitBounds(
        [startLng, startLat],
        [destLng, destLat],
        [80, 80, 200, 200]
      );
    } catch (err) {
      console.error(err);
      alert("Error building route");
    }
  };

  const handleBuildRoute = async () => {
    Keyboard.dismiss();
    closeMenu();
    setTimeout(buildRoute, 350);
  };

  const clearRoute = () => {
    setRouteCoords(null);
    setEta(null);
    setDistance(null);
    setNextStep(null);
    setDestination(null);
    setDestinationName("");
    setSuggestions([]);

    Animated.timing(opacityAnim, {
      toValue: 0,
      duration: 400,
      useNativeDriver: true,
    }).start();
  };

  return (
    <View style={styles.container}>
      <Mapbox.MapView
        styleURL="mapbox://styles/mapbox/navigation-night-v1"
        style={styles.map}
        logoEnabled={false}
        attributionEnabled={false}
        scaleBarEnabled={false}
      >
        <Mapbox.Camera ref={cameraRef} zoomLevel={14} centerCoordinate={userCoords || [0, 0]} />

        <Mapbox.UserLocation visible={true} androidRenderMode="normal" showsUserHeadingIndicator />

        {routeCoords && (
          <Mapbox.ShapeSource
            id="routeSource"
            shape={{
              type: "Feature",
              geometry: { type: "LineString", coordinates: routeCoords },
              properties: {},
            }}
          >
            <Mapbox.LineLayer
              id="routeLine"
              style={{
                lineColor: "#3B82F6",
                lineWidth: 5,
                lineJoin: "round",
                lineCap: "round",
              }}
            />
          </Mapbox.ShapeSource>
        )}

        {destination && (
          <Mapbox.PointAnnotation id="dest" coordinate={destination}>
            <View style={styles.destMarkerOuter}>
              <View style={styles.destMarkerInner} />
            </View>
          </Mapbox.PointAnnotation>
        )}
      </Mapbox.MapView>

      {eta && distance && (
        <Animated.View style={[styles.navigationBanner, { opacity: opacityAnim }]}>
          <View style={styles.navTopRow}>
            <Text style={styles.etaText}>{eta}</Text>
            <Text style={styles.distanceText}>{distance}</Text>
          </View>
          <View style={styles.navDivider} />
          <View style={styles.nextStepRow}>
            <Text style={styles.turnIcon}>⬅️</Text>
            <Text style={styles.nextInstruction}>{nextStep || "Next turn soon"}</Text>
          </View>
        </Animated.View>
      )}

      <Animated.View
        {...panResponder.panHandlers}
        style={[styles.bottomSheet, { transform: [{ translateY }] }]}
      >
        <Pressable onPress={handlePress} style={{ flex: 1 }}>
          <View style={styles.handleContainer}>
            <View style={styles.hiddenHandle} />
          </View>

          <Animated.View
            style={{
              opacity: isExpanded ? 1 : 0,
              height: isExpanded ? "100%" : 0,
              overflow: "hidden",
            }}
          >
            <ScrollView style={styles.expandedContent} keyboardShouldPersistTaps="handled">
              <Text style={styles.sheetTitle}>{user?.userName || "Ghost"}</Text>

              {/* Transport mode selector */}
              <View style={styles.routeTypeContainer}>
                {[{ id: "driving", label: "🚗 Car" }, { id: "walking", label: "🚶 Walk" }].map(
                  (mode) => (
                    <TouchableOpacity
                      key={mode.id}
                      style={[
                        styles.routeTypeButton,
                        routeType === mode.id && styles.routeTypeButtonActive,
                      ]}
                      onPress={() => setRouteType(mode.id as any)}
                    >
                      <Text
                        style={[
                          styles.routeTypeText,
                          routeType === mode.id && styles.routeTypeTextActive,
                        ]}
                      >
                        {mode.label}
                      </Text>
                    </TouchableOpacity>
                  )
                )}
              </View>

              {/* Destination input field */}
              <TextInput
                style={styles.input}
                placeholder="Enter destination..."
                placeholderTextColor="#9ca3af"
                value={destinationName}
                onChangeText={setDestinationName}
              />

              {/* Autocomplete suggestion list */}
              {suggestions.length > 0 && (
                <ScrollView
                  style={styles.suggestionList}
                  keyboardShouldPersistTaps="handled"
                >
                  {suggestions.map((item) => (
                    <TouchableOpacity
                      key={item.id}
                      style={styles.suggestionItem}
                      onPress={(e) => handleSelectSuggestion(item, e)}
                    >
                      <Text style={styles.suggestionText}>{item.place_name}</Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              )}

              {/* Action buttons */}
              <TouchableOpacity style={styles.buildButton} onPress={handleBuildRoute}>
                <Text style={styles.buildButtonText}>🧭 Build Route</Text>
              </TouchableOpacity>

              <TouchableOpacity style={styles.centerButton} onPress={centerCamera}>
                <Text style={styles.buildButtonText}>🎯 Center on Me</Text>
              </TouchableOpacity>

              {/* Group buttons */}
              <View style={styles.groupButtonsContainer}>
                <TouchableOpacity style={styles.makeGroupButton} onPress={makeGroupButton}>
                  <Text style={styles.buildButtonText}>👥 Group</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.addToGroupButton} onPress={addToGroupButton}>
                  <Text style={styles.buildButtonText}>➕ Add to Group</Text>
                </TouchableOpacity>
              </View>

              {routeCoords && (
                <TouchableOpacity style={styles.deleteButton} onPress={clearRoute}>
                  <Text style={styles.deleteButtonText}>🗑 Delete Route</Text>
                </TouchableOpacity>
              )}
            </ScrollView>
          </Animated.View>
        </Pressable>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  map: { flex: 1 },
  navigationBanner: {
    position: "absolute",
    top: 55,
    left: 20,
    right: 20,
    backgroundColor: "rgba(17, 24, 39, 0.85)",
    borderRadius: 16,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderColor: "rgba(59,130,246,0.3)",
    shadowColor: "#000",
    shadowOpacity: 0.4,
    shadowRadius: 10,
    elevation: 8,
  },
  navTopRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  etaText: { color: "#fff", fontSize: 22, fontWeight: "700" },
  distanceText: { color: "#9ca3af", fontSize: 18, fontWeight: "500" },
  navDivider: {
    height: 1,
    backgroundColor: "rgba(255,255,255,0.15)",
    marginVertical: 8,
  },
  nextStepRow: { flexDirection: "row", alignItems: "center" },
  turnIcon: { fontSize: 22, marginRight: 10, color: "#60A5FA" },
  nextInstruction: { color: "#E5E7EB", fontSize: 16 },
  destMarkerOuter: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: "rgba(34,197,94,0.3)",
    alignItems: "center",
    justifyContent: "center",
  },
  destMarkerInner: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: "#22c55e",
    borderWidth: 2,
    borderColor: "white",
  },
  bottomSheet: {
    position: "absolute",
    left: 0,
    right: 0,
    height: EXPANDED_HEIGHT,
    backgroundColor: "#1f2937",
    borderTopLeftRadius: 25,
    borderTopRightRadius: 25,
    overflow: "hidden",
    paddingHorizontal: 20,
  },
  handleContainer: { alignItems: "center", paddingTop: 10, paddingBottom: 5 },
  hiddenHandle: {
    width: 70,
    height: 8,
    borderRadius: 4,
    backgroundColor: "#fff",
  },
  expandedContent: { paddingTop: 10 },
  sheetTitle: {
    color: "#fff",
    alignSelf: "center",
    fontSize: 22,
    fontWeight: "700",
    marginBottom: 16,
  },
  input: {
    backgroundColor: "#374151",
    color: "#fff",
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 16,
  },
  suggestionList: {
    backgroundColor: "#2d3748",
    borderRadius: 8,
    marginTop: 8,
    maxHeight: 180,
  },
  suggestionItem: {
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderBottomColor: "#4b5563",
    borderBottomWidth: 1,
  },
  suggestionText: { color: "#e5e7eb", fontSize: 15 },
  buildButton: {
    backgroundColor: "#3B82F6",
    paddingVertical: 14,
    borderRadius: 10,
    marginTop: 14,
    alignItems: "center",
  },
  buildButtonText: { color: "white", fontSize: 17, fontWeight: "600" },
  centerButton: {
    backgroundColor: "#10B981",
    paddingVertical: 14,
    borderRadius: 10,
    marginTop: 10,
    alignItems: "center",
  },
  groupButtonsContainer: {
    flexDirection: "row",
    justifyContent: "space-between",
    width: "100%",
    marginTop: 10,
  },
  makeGroupButton: {
    backgroundColor: "#4F8EF7",
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: "center",
    width: "48%",
  },
  addToGroupButton: {
    backgroundColor: "#36C36E",
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: "center",
    width: "48%",
  },
  deleteButton: {
    backgroundColor: "#EF4444",
    paddingVertical: 14,
    borderRadius: 10,
    marginTop: 10,
    alignItems: "center",
  },
  deleteButtonText: { color: "white", fontSize: 17, fontWeight: "600" },
  routeTypeContainer: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 12,
  },
  routeTypeButton: {
    flex: 1,
    backgroundColor: "#374151",
    marginHorizontal: 4,
    borderRadius: 10,
    paddingVertical: 10,
    alignItems: "center",
  },
  routeTypeButtonActive: { backgroundColor: "#3B82F6" },
  routeTypeText: { color: "#9ca3af", fontSize: 16 },
  routeTypeTextActive: { color: "white", fontWeight: "600" },
});
