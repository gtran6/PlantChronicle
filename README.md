# Plant Chronicle Project
---
## Introduction

Did you plant an apple tree and struggle to remember when to water and fertilize it? 
Plant Chronicle can assist you with:
- Keeping track of the dates and locations where you planted your plants.
- Taking and viewing photos of your plants at different stages of their life.
- Recording when you added water, fertilizer, and other amendments to the plants.
- Being informed about upcoming events for your plants such as when to water, when the growing season ends, etc.   

Using your Android device to create a personalized plant diary. Capturing photos with the on-device camera and setting reminders based on previous years' experiences. Receiving alerts about upcoming events for your plants.  

### Phone Display
<img src="https://user-images.githubusercontent.com/78507684/232845521-0be4cec9-4831-41f3-a750-2ed53ba7cbe9.png" width="30%" height="30%">&ensp;

## Functional Requirements

### take picture of plants
<img src="https://user-images.githubusercontent.com/78507684/232845612-bb92fc0c-a57f-42f7-a82d-8bc874b2c68c.webm" width="30%" height="30%">

### requirement 100.0: search for plants

#### Scenario 

As a plant lover, I want to explore plants by using any part of their name, including the genus, species, cultivar, or common name.

#### Dependencies

Plant search data are available and accessible


#### Assumptions

Scientific names are stated in Latin.

Common names are stated in English.

#### Examples

1.1
Given a feed of plant data is available
Given GPS details are available
When

- Select the plant Asimina triloba
- Add notes: “planted by Giang Tran”  

Then when I navigate to the Specimen History view, I should see at least one Asimina triloba specimen with the notes, “planted by Giang Tran”

2.1
Given a feed of plant data is available
Given GPS details are available
When

- Select the plant Malus domestica ‘Fuji’
- Take a photo of a Fuji apple seedling 

Then when I navigate to the Specimen History view, I should see at least one Malus domestica ‘Fuji’ specimen with the a photo of a Fuji apple seedling.

### Developed By
Giang Tran


