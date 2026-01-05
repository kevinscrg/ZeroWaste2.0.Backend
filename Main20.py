import pandas as pd
import re
import numpy as np
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import MultiLabelBinarizer
import json
import stomp
import time
import queue
from concurrent.futures import ThreadPoolExecutor

request_queue = queue.Queue()

class SpringStompListener(stomp.ConnectionListener):
    def __init__(self, conn):
        self.conn = conn

    def on_message(self, frame):
        try:
            cerere = json.loads(frame.body)
            request_queue.put(cerere)
        except Exception as e:
            print(f"Error parseando JSON: {e}")

    def on_error(self, frame):
        print(f"STOMP Error: {frame.body}")

    def on_disconnected(self):
        print("Disconnected from STOMP server. Shuting down...")
        return super().on_disconnected()

def filter_recipes(user_allergies, user_preferences, user_type, user_difficulty, user_time):
    # Citirea datelor
    file_path = 'Files/recipesAllergensPreferences.xlsx'
    df = pd.read_excel(file_path, sheet_name='AI')

    """ TIMPI DE GATIRE
     1 - pentru retete cu timp de gatire maxim 30 min (inclusiv)
     2 - max 60 min
     3 - max 120 min
     4 - max 180 min """

    allergen_columns = [col for col in df.columns if col in ['Celery', 'Cereals', 'Crustaceans', 'Eggs', 'Fish', 'Lupin', 'Milk', 'Molluscs', 'Mustard', 'Peanuts', 'Sesame', 'Soybeans', 'Sulphur']]
    preference_columns = [col for col in df.columns if col in ['Dairy-Free', 'Gluten-Free', 'Vegan', 'Vegetarian']]
    valid_allergies = [allergen for allergen in user_allergies if allergen in allergen_columns]
    valid_preferences = [preference for preference in user_preferences if preference in preference_columns]

    filtered_recipes = df.copy()

    # Filtrare pe baza alergenilor
    if valid_allergies:
        for allergen in valid_allergies:
            filtered_recipes = filtered_recipes[filtered_recipes[allergen] == 0]

    # Filtrare pe baza preferințelor
    if valid_preferences:
        preference_mask = filtered_recipes[valid_preferences].any(axis=1)
        filtered_recipes = filtered_recipes[preference_mask]

    # Filtrare pe baza tipului
    if user_type:
        filtered_recipes = filtered_recipes[filtered_recipes['Type'].isin(user_type)]

    # Filtrare pe baza dificultății
    if user_difficulty:
        filtered_recipes = filtered_recipes[filtered_recipes['Difficulty'].isin(user_difficulty)]

    if user_time is not None:
        time_limits = {1: 30, 2: 60, 3: 120, 4: 180}
        if user_time in time_limits:
            max_time = time_limits[user_time]
            filtered_recipes = filtered_recipes[filtered_recipes['Total time'] <= max_time]
    return filtered_recipes


def use_expiring_ingredients(recipes_df, expiring_products):

    # Lista de produse care expiră ---- PRIMUL INGREDIENT E CEL CARE EXPIRA CEL MAI REPEDE

    # Normalizarea listei de produse care expiră la litere mici
    expiring_products = [product.lower() for product in expiring_products]

    # Funcție pentru a calcula numărul de potriviri între ingrediente și expiring_products,
    # excluzând cazurile cu "mini mozzarella"
    def count_matches_excluding(ingredients, expiring_products):
        ingredients_lower = ingredients.lower()
        count = 0
        for product in expiring_products:
            if product in ingredients_lower and f'mini {product}' not in ingredients_lower:
                count += 1
        return count

    # Funcție pentru a returna poziția ingredientului de prioritate cea mai mare,
    # excluzând cazurile cu "mini mozzarella"
    def highest_priority_match_excluding(ingredients, expiring_products):
        ingredients_lower = ingredients.lower()
        for idx, product in enumerate(expiring_products):
            if product in ingredients_lower and f'mini {product}' not in ingredients_lower:
                return idx  # Returnăm poziția primului ingredient găsit
        return len(expiring_products)  # Dacă nu se găsește niciunul, returnăm o valoare mare

    # Calcularea numărului de potriviri și a priorității maxime pentru fiecare rețetă
    recipes_df['Match Count'] = recipes_df['Ingredients'].apply(
        lambda x: count_matches_excluding(str(x), expiring_products)
    )
    recipes_df['Priority'] = recipes_df['Ingredients'].apply(
        lambda x: highest_priority_match_excluding(str(x), expiring_products)
    )

    #sortarea rețetelor
    sorted_recipes = recipes_df.sort_values(by=['Match Count', 'Priority'], ascending=[False, True])

    return sorted_recipes



def preprocess_ingredients(text):
    return re.sub(r'\b\d+\b', '', text)

def recomendations(retete_df, user_liked_recipe_ids, user_disliked_recipe_ids):
    # Setăm ID-ul ca index pentru referințe mai ușoare
    retete_df = retete_df.set_index('id', drop=False)

    # Preprocesare ingrediente
    retete_df['Processed_Ingredients'] = retete_df['Ingredients'].apply(preprocess_ingredients)
    retete_df['Ingredients_List'] = retete_df['Processed_Ingredients'].apply(
        lambda x: [item.strip().lower() for item in x.split(',')]
    )

    # Validare ID-uri
    valid_liked_ids = [rid for rid in user_liked_recipe_ids if rid in retete_df.index]
    valid_disliked_ids = [rid for rid in user_disliked_recipe_ids if rid in retete_df.index]

    if not valid_liked_ids and not valid_disliked_ids:
        print("Nu există rețete plăcute sau respinse valide pentru utilizator. Returnăm toate rețetele disponibile.")
        return retete_df.reset_index(drop=True)

    # Excludem rețetele respinse
    retete_df = retete_df[~retete_df.index.isin(valid_disliked_ids)]

    # Vectorizare
    mlb = MultiLabelBinarizer()
    ingredient_matrix = mlb.fit_transform(retete_df['Ingredients_List'])

    vectorizer = CountVectorizer()
    word_matrix = vectorizer.fit_transform(retete_df['Processed_Ingredients'])

    combined_matrix = np.hstack([ingredient_matrix, word_matrix.toarray()])

    # Obținem indicii pentru ID-uri plăcute
    liked_indices = [retete_df.index.get_loc(rid) for rid in valid_liked_ids if rid in retete_df.index]

    if not liked_indices:
        print("Nu există potriviri pentru rețetele plăcute de utilizator. Returnăm rețetele fără dislike-uri.")
        return retete_df.reset_index(drop=True)

    # Calcul similaritate
    similarities = cosine_similarity(combined_matrix[liked_indices], combined_matrix)
    average_similarity = similarities.mean(axis=0)
    retete_df['similarity'] = average_similarity

    # Sortare
    recommended_recipes = retete_df.sort_values(by='similarity', ascending=False)

    return recommended_recipes.reset_index(drop=True) if not recommended_recipes.empty else retete_df.reset_index(drop=True)



def process_requests_worker(conn):
    while True:
        try:
            cerere = request_queue.get()
            payload = cerere.get('payload', {})

            print(f"Received request with payload: {payload}")

            email = payload.get('email')
            user_allergies = payload.get('Allergens', [])
            user_preferences = payload.get('Preferences', [])
            user_difficulty = payload.get('Difficulty')
            user_time = payload.get('Time')
            user_type = payload.get('Type')
            user_liked_recipe_ids = payload.get('LikedRecipes', [])
            user_disliked_recipe_ids = payload.get('DislikedRecipes', [])
            expiring_products = payload.get('ExpiringProducts')

            filtered = filter_recipes(user_allergies, user_preferences, user_type, user_difficulty, user_time)
            
            if expiring_products:
                filtered = use_expiring_ingredients(filtered, expiring_products)
            
            if user_liked_recipe_ids:
                filtered = recomendations(filtered, user_liked_recipe_ids, user_disliked_recipe_ids)

            recipe_ids = filtered["id"].tolist()
            
            response = {
                "type": "run", 
                "payload": {"recipe_ids": recipe_ids, "email": email}
            }
            
            conn.send(
                body=json.dumps(response), 
                destination="/app/python-response",
                headers={'content-type': 'application/json'}
            )
            
            request_queue.task_done()
        except Exception as e:
            print(f"Worker error: {e}")

def main():
    
    conn = stomp.WSConnection(host_and_ports=[("localhost", 8080)], ws_path="/ws-endpoint")
    conn.set_listener('spring_listener', SpringStompListener(conn))
    
    try:
        conn.connect(wait=True)
        conn.subscribe(destination='/topic/python-requests', id="sub-python", ack='auto')
        print("Running and waiting for requests...")

        num_threads = 5
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            for _ in range(num_threads):
                executor.submit(process_requests_worker, conn)
            
            while True:
                time.sleep(1)
                
    except KeyboardInterrupt:
        print("Shutting down...")
    finally:
        if conn.is_connected():
            conn.disconnect()

if __name__ == "__main__":
    main()