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
import { useSafeAreaInsets } from "react-native-safe-area-context";

// Mapbox API access token
const MAPBOX_TOKEN =
  "sk.eyJ1IjoiYXBrYXN2IiwiYSI6ImNtaHBkcXYyMjBqNHMyaXBnZzUzaDF1NmsifQ.YerdtQnb2UN4CEZOFqxxQA";

Mapbox.setAccessToken(MAPBOX_TOKEN);

const { height } = Dimensions.get("window");
const COLLAPSED_HEIGHT = 80;
const EXPANDED_HEIGHT = height * 0.82;


export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const [userCoords, setUserCoords] = useState<number[] | null>(null);
  const [destination, setDestination] = useState<number[] | null>(null);
  const [destinationName, setDestinationName] = useState("");
  const [suggestions, setSuggestions] = useState<any[]>([]);
  const [routeType, setRouteType] = useState<"driving" | "walking">("driving");
  const [routeCoords, setRouteCoords] = useState<number[][] | null>(null);
  const [routeProgress, setRouteProgress] = useState<number>(0);
  const [routeMeta, setRouteMeta] = useState<
    | {
        cumDist: number[]; // cumulative distance along route polyline (m)
        totalDist: number; // total route distance (m)
        speed: number; // seconds per meter (avg over route)
        steps: any[]; // steps from Mapbox directions
        stepCoordIdx: number[]; // mapping of step maneuver to nearest route coord index
      }
    | null
  >(null);
  const [eta, setEta] = useState<string | null>(null);
  const [distance, setDistance] = useState<string | null>(null);
  const [nextStep, setNextStep] = useState<string | null>(null);
  const [nextTurnArrow, setNextTurnArrow] = useState<string>("↑");
  const [committedPlaceName, setCommittedPlaceName] = useState<string | null>(null);
  const [group, setGroup] = useState<any | null>(null);
  const [isCreatingGroup, setIsCreatingGroup] = useState(false);
  const [groupError, setGroupError] = useState<string | null>(null);


  const translateY = useRef(new Animated.Value(height - COLLAPSED_HEIGHT)).current;
  const [isExpanded, setIsExpanded] = useState(false);
  const cameraRef = useRef<Mapbox.Camera>(null);
  const opacityAnim = useRef(new Animated.Value(0)).current;

  // GPS noise filter helpers
  const lastAcceptedCoordsRef = useRef<number[] | null>(null);
  const MIN_MOVE_METERS = 10; // ignore smaller jiggle
  const toRad = (x: number) => (x * Math.PI) / 180;
  const distanceMeters = (a: number[], b: number[]) => {
    const [lon1, lat1] = a;
    const [lon2, lat2] = b;
    const R = 6371000; // meters
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const la1 = toRad(lat1);
    const la2 = toRad(lat2);
    const sinDLat = Math.sin(dLat / 2);
    const sinDLon = Math.sin(dLon / 2);
    const h = sinDLat * sinDLat + Math.cos(la1) * Math.cos(la2) * sinDLon * sinDLon;
    const c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    return R * c;
  };

  // Find nearest index along a polyline to a given coord
  const nearestCoordIndex = (poly: number[][], p: number[]) => {
    let minD = Infinity;
    let minI = 0;
    for (let i = 0; i < poly.length; i++) {
      const d = distanceMeters(poly[i], p);
      if (d < minD) {
        minD = d;
        minI = i;
      }
    }
    return minI;
  };

  // Map Mapbox maneuver to a plain Unicode arrow
  const arrowForManeuver = (man?: any): string => {
    const mod = man?.modifier?.toLowerCase?.() as string | undefined;
    const type = man?.type?.toLowerCase?.() as string | undefined;
    if (mod === "left") return "←";
    if (mod === "right") return "→";
    if (mod === "slight left") return "↖";
    if (mod === "slight right") return "↗";
    if (mod === "sharp left") return "↙";
    if (mod === "sharp right") return "↘";
    if (mod === "uturn" || type === "uturn") return "↺";
    if (type === "arrive") return "↑";
    return "↑"; // continue/unknown
  };


  interface User {
    userName: string;
    id: string;
    groupId?: string;
    numericId?: number;
  }

  const [user, setUser] = useState<User | null>(null);

  // Register user on-demand if not authorized
  const registerIfUnauthed = async (): Promise<User | null> => {
    if (user) return user;
    try {
      const res = await fetch(
        "https://parf-api.up.railway.app/api/auth/register",
        {
          method: "POST",
          headers: { "Content-Type": "application/json", Accept: "application/json" },
        }
      );
      if (!res.ok) {
        console.error("Register failed", res.status, res.statusText);
        return null;
      }
      const data = (await res.json()) as any;
      try { console.log("register response", data); } catch {}
      const possibleIds = [data?.id, data?.userId, data?.userID, data?.user_id];
      const numericId = possibleIds
        .map((v: any) => (v !== null && v !== undefined ? Number(v) : NaN))
        .find((n: number) => Number.isFinite(n));
      const newUser: User = {
        id: String(data?.userId ?? data?.id ?? ""),
        userName: String(data?.username ?? data?.name ?? "User"),
        ...(Number.isFinite(numericId) ? { numericId } : {}),
      };
      setUser(newUser);
      return newUser;
    } catch (e) {
      console.error("Register error", e);
      return null;
    }
  };


  const isFiniteNumber = (n: any) => typeof n === 'number' && Number.isFinite(n);
  const isValidPosition = (p: any) => p && isFiniteNumber(p.longitude) && isFiniteNumber(p.latitude);


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
      const coords = [lastKnown.coords.longitude, lastKnown.coords.latitude] as number[];
      setUserCoords(coords);
      lastAcceptedCoordsRef.current = coords;
    }

    // if not - request again
    const current = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.High,
    });
    const currentCoords = [current.coords.longitude, current.coords.latitude] as number[];
    setUserCoords(currentCoords);
    lastAcceptedCoordsRef.current = currentCoords;

    // after sucsess - folowing actual geolocation data
    await Location.watchPositionAsync(
      {
        accuracy: Location.Accuracy.High,
        distanceInterval: 10,
        timeInterval: 2000,
      },
      (loc) => {
        const coords = [loc.coords.longitude, loc.coords.latitude] as number[];
        const last = lastAcceptedCoordsRef.current;
        if (!last || distanceMeters(last, coords) >= MIN_MOVE_METERS) {
          setUserCoords(coords);
          lastAcceptedCoordsRef.current = coords;

          // If a route is active, update live progress (ETA, remaining distance, next step)
          if (routeCoords && routeMeta) {
            try {
              const idx = nearestCoordIndex(routeCoords, coords);
              const progressed = routeMeta.cumDist[idx];
              const remaining = Math.max(0, routeMeta.totalDist - progressed);
              setRouteProgress(routeMeta.totalDist > 0 ? progressed / routeMeta.totalDist : 0);
              const minutes = Math.max(1, Math.round((remaining * routeMeta.speed) / 60));
              setDistance((remaining / 1000).toFixed(1) + " km");
              setEta(minutes + " min");

              // Determine next maneuver based on nearest coord index
              let nextInstruction: string | null = null;
              for (let s = 0; s < routeMeta.stepCoordIdx.length; s++) {
                if (routeMeta.stepCoordIdx[s] > idx) {
                  const man = routeMeta.steps[s]?.maneuver;
                  const instr = man?.instruction;
                  nextInstruction = instr ? String(instr) : null;
                  setNextTurnArrow(arrowForManeuver(man));
                  break;
                }
              }
              setNextStep(nextInstruction || nextStep);

              // Auto-complete route if we are within ~30m of the end
              const end = routeCoords[routeCoords.length - 1];
              if (distanceMeters(end, coords) < 30) {
                // Clear route on arrival
                clearRoute();
              }
            } catch {}
          }
        }
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

  // Auto recenters to user after 10s of no map movement when a route is active
  const recenterTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const clearRecenterTimer = () => {
    if (recenterTimerRef.current) {
      clearTimeout(recenterTimerRef.current);
      recenterTimerRef.current = null;
    }
  };
  const skipNextRegionRef = useRef(false);
  const recenterToUser = () => {
    if (!userCoords) return;
    skipNextRegionRef.current = true;
    cameraRef.current?.setCamera({
      centerCoordinate: userCoords,
      zoomLevel: 20,
      animationDuration: 1000,
    });
  };
  
  const scheduleRecenter = () => {
    if (!routeCoords) return;
    clearRecenterTimer();
    recenterTimerRef.current = setTimeout(() => {
      recenterToUser();
    }, 10000);
  };

  // Clear timer on unmount
  useEffect(() => {
    return () => clearRecenterTimer();
  }, []);

  const makeGroupButton = async () => {
    if (isCreatingGroup || group) return;
    setGroupError(null);
    setIsCreatingGroup(true);

    try {
      const currentUser = await registerIfUnauthed();
      if (!currentUser) {
        setGroupError("Unable to authorize user");
        return;
      }

      // Backend is stateless; no session cookie involved.

      const destObj = destination
        ? { longitude: destination[0], latitude: destination[1] }
        : { longitude: 43.165, latitude: 52.459 };

      const hostPosObj = userCoords
        ? { longitude: userCoords[0], latitude: userCoords[1] }
        : { longitude: 41.165, latitude: 54.459 };

      // Validate coordinates to avoid backend 500 due to bad numbers
      if (!isValidPosition(destObj)) {
        setGroupError("Invalid destination coordinates");
        return;
      }
      if (!isValidPosition(hostPosObj)) {
        setGroupError("Invalid host position coordinates");
        return;
      }

      // API requires hostId to be a UUID string
      const payload: any = {
        hostId: String(currentUser.id),
        destination: destObj,
        hostPos: hostPosObj,
      };

      console.log("groupMake payload", payload);
      const endpoints = [
        "https://parf-api.up.railway.app/api/trip/groupMake",
      ];

      let data: any | null = null;
      let lastStatus: number | null = null;
      let lastBody: string | null = null;

      for (const url of endpoints) {
        try {
          const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json", Accept: "application/json" },
            body: JSON.stringify(payload),
          });
          lastStatus = res.status;
          if (res.ok) {
            data = await res.json();
            break;
          } else {
            try {
              // Try parse error JSON for message
              const text = await res.text();
              lastBody = text;
              try {
                const j = JSON.parse(text);
                if (j?.message) console.warn("groupMake server message:", j.message);
                if (j?.error) console.warn("groupMake server error:", j.error);
              } catch {}
            } catch {}
            if (res.status !== 404) break;
          }
        } catch (err) {
          console.error("Group make fetch error for endpoint", err);
        }
      }

      if (!data) {
        if (lastBody) console.log("groupMake error body:", lastBody);
        const msg = lastStatus === 404
          ? "Endpoint not found (404). Please verify API route."
          : lastStatus === 400
          ? "Bad request (400). Check hostId and coordinates."
          : lastStatus === 401
          ? "Unauthorized (401). Session cookie missing or invalid."
          : lastStatus === 500
          ? "Server error (500). Please verify backend can resolve userId and parse positions."
          : `Group creation failed (${lastStatus ?? "network"})`;
        setGroupError(msg);
        return;
      }

      // success
      setGroup(data);
      const groupCode = data?.groupCode ?? data?.code;
      setUser({ ...currentUser, groupId: groupCode ?? currentUser.groupId });
    } catch (e) {
      console.error("Group make error", e);
      setGroupError("Error creating group");
    } finally {
      setIsCreatingGroup(false);
    }
  };

  const addToGroupButton = async () => {
    if (!user) {
      await registerIfUnauthed();
      console.log("addToGroupButton is pressed with register fetch ");
    }
    console.log("addToGroupButton is pressed ");
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
        const features = Array.isArray(data.features) ? data.features : [];

        // If user selected a suggestion and the text matches it exactly, keep the list closed
        if (committedPlaceName && destinationName === committedPlaceName) {
          return setSuggestions([]);
        }
        // If there is an exact match among suggestions, close the list to prevent reopen
        const hasExact = features.some((f: any) => String(f.place_name) === destinationName);
        setSuggestions(hasExact ? [] : features);
      } catch (err) {
        console.error("Autocomplete error", err);
      }
    }, 400);
    return () => clearTimeout(delay);
  }, [destinationName, committedPlaceName]);

  const handleSelectSuggestion = (item: any, e?: any) => {
    e?.stopPropagation?.();
    const coords = item.center;
    setDestination(coords);
    setDestinationName(item.place_name);
    setCommittedPlaceName(item.place_name);
    setSuggestions([]);
  };

  const handleDestinationChange = (text: string) => {
    setDestinationName(text);
    if (committedPlaceName && text !== committedPlaceName) {
      setCommittedPlaceName(null);
    }
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
      const coords = route.geometry.coordinates as number[][];
      setRouteCoords(coords);

      // Build cumulative distance array along the route
      const cum: number[] = new Array(coords.length).fill(0);
      for (let i = 1; i < coords.length; i++) {
        cum[i] = cum[i - 1] + distanceMeters(coords[i - 1], coords[i]);
      }
      const totalDist = cum[cum.length - 1] || route.distance || 0;

      // Average speed (seconds per meter)
      const speed = totalDist > 0 ? (route.duration as number) / totalDist : 0.8;

      // Map maneuvers to nearest indices for step lookup as we progress
      const steps = (route.legs?.[0]?.steps || []) as any[];
      const stepCoordIdx = steps.map((s) => {
        const loc = s?.maneuver?.location as number[] | undefined;
        if (!loc || loc.length !== 2) return coords.length - 1;
        return nearestCoordIndex(coords, [loc[0], loc[1]]);
      });

      setRouteMeta({ cumDist: cum, totalDist, speed, steps, stepCoordIdx });

      // Initial ETA/distance and first instruction
      setDistance((totalDist / 1000).toFixed(1) + " km");
      setEta(Math.max(1, Math.round((totalDist * speed) / 60)) + " min");
      const firstMan = steps?.[0]?.maneuver;
      const firstStep = firstMan?.instruction;
      if (firstStep) setNextStep(String(firstStep));
      setNextTurnArrow(arrowForManeuver(firstMan));
      setRouteProgress(0);

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

      // After showing route overview, schedule auto-recenter if idle
      scheduleRecenter();
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
    setRouteMeta(null);
    setRouteProgress(0);
    setEta(null);
    setDistance(null);
    setNextStep(null);
    setDestination(null);
    setDestinationName("");
    setCommittedPlaceName(null);
    setSuggestions([]);

    // Stop any pending recenters when route cleared
    clearRecenterTimer();

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
        onRegionDidChange={() => {
          if (skipNextRegionRef.current) {
            skipNextRegionRef.current = false;
            return;
          }
          scheduleRecenter();
        }}
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
        <Animated.View
          style={[
            styles.navigationBanner,
            { opacity: opacityAnim, top: insets.top},
          ]}
        >
          <View style={styles.progressTrack}>
            <View style={[styles.progressFill, { width: `${Math.max(0, Math.min(100, routeProgress * 100))}%` }]} />
          </View>
          <View style={styles.navTopRow}>
            <Text style={styles.etaText}>{eta}</Text>
            <Text style={styles.distanceText}>{distance}</Text>
          </View>
          <View style={styles.nextStepRow}>
            <Text style={styles.turnIcon}>{nextTurnArrow}</Text>
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

          {!isExpanded && (
            <View style={styles.collapsedUserContainer}>
              <Text style={styles.collapsedUserName}>{user?.userName || "Guest"}</Text>
            </View>
          )}

          <Animated.View
            style={{
              opacity: isExpanded ? 1 : 0,
              height: isExpanded ? "100%" : 0,
              overflow: "hidden",
            }}
          >
            <ScrollView style={styles.expandedContent} keyboardShouldPersistTaps="handled">
              <Text style={styles.sheetTitle}>{user?.userName || "Guest"}</Text>

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
                onChangeText={handleDestinationChange}
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
                <TouchableOpacity
                  style={[styles.makeGroupButton, isCreatingGroup && { opacity: 0.7 }]}
                  disabled={isCreatingGroup}
                  onPress={makeGroupButton}
                >
                  <Text style={styles.buildButtonText}>👥 Group</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.addToGroupButton} onPress={addToGroupButton}>
                  <Text style={styles.buildButtonText}>➕ Join to Group</Text>
                </TouchableOpacity>
              </View>

              {groupError && (
                <Text style={styles.errorText}>{groupError}</Text>
              )}

              {group && (
                <View style={styles.groupInfoCard}>
                  <Text style={styles.groupCodeBig}>
                    {String(group?.code ?? group?.groupCode ?? "")}
                  </Text>
                  <View style={{ marginTop: 8 }}>
                    {(() => {
                      const baseMembers: any[] =
                        Array.isArray(group?.members) && group.members.length > 0
                          ? group.members
                          : [];
                      const membersForDisplay =
                        baseMembers.length > 0
                          ? baseMembers
                          : [
                              user
                                ? { id: user.id, userName: user.userName }
                                : { id: "me", userName: "Me" },
                            ];
                      return membersForDisplay.map((m: any, idx: number) => {
                        let displayName: string | null = null;
                        if (m && typeof m === "object") {
                          displayName =
                            (m.userName ?? m.username ?? m.name ?? m.displayName) ?? null;
                        }
                        // If entry is just an id or missing name, try to use current user's userName
                        if (!displayName) {
                          const mid = m && typeof m === "object" ? (m as any).id : m;
                          if (
                            user &&
                            user.id !== undefined &&
                            String(mid ?? "") === String(user.id) &&
                            user.userName
                          ) {
                            displayName = user.userName;
                          }
                        }
                        if (!displayName) displayName = `Member ${idx + 1}`;
                        return (
                          <Text key={String((m && (m.id ?? m)) ?? idx)} style={styles.groupMember}>
                            {String(displayName)}
                          </Text>
                        );
                      });
                    })()}
                  </View>
                </View>
              )}

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
    top: 12,
    left: 12,
    right: 12,
    backgroundColor: "rgba(12,12,14,0.9)",
    borderRadius: 18,
    paddingVertical: 10,
    paddingHorizontal: 14,
    shadowColor: "#000",
    shadowOpacity: 0.25,
    shadowRadius: 12,
    elevation: 10,
  },
  navTopRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  etaText: { color: "#fff", fontSize: 24, fontWeight: "700" },
  distanceText: { color: "#9ca3af", fontSize: 16, fontWeight: "600" },
  nextStepRow: { flexDirection: "row", alignItems: "center", marginTop: 2 },
  turnIcon: { fontSize: 28, marginRight: 8, color: "#93C5FD" },
  nextInstruction: { color: "#E5E7EB", fontSize: 15 },
  progressTrack: {
    height: 3,
    backgroundColor: "rgba(255,255,255,0.08)",
    borderRadius: 2,
    overflow: "hidden",
    marginBottom: 8,
  },
  progressFill: {
    height: 3,
    backgroundColor: "#3B82F6",
  },
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
  collapsedUserContainer: { alignItems: "center", paddingBottom: 8 },
  collapsedUserName: { color: "#fff", fontSize: 16, fontWeight: "600" },
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
  errorText: { color: "#FCA5A5", marginTop: 8, fontSize: 14 },
  groupInfoCard: {
    backgroundColor: "#111827",
    borderColor: "rgba(59,130,246,0.3)",
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    marginTop: 10,
  },
  groupCodeBig: { color: "#fff", fontSize: 30, fontWeight: "800", textAlign: "center" },
  groupMember: { color: "#e5e7eb", fontSize: 16, lineHeight: 22 },
});
