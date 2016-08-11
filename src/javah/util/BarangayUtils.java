package javah.util;

import java.util.ArrayList;
import java.util.List;

public class BarangayUtils {

    /**
     * Filter the lists in ascending order, priority level not ignored,with regards to the keywords.
     * @param residentIds
     * @param residentNames
     * @param keywords
     * @return a new filtered lists of resident IDs List[0] and resident names List[1].
     */
    public static List[] filterLists(List<String> residentIds, List<String> residentNames, String[] keywords) {

        int listSize = residentIds.size();

        // Store the priority level of each residents.
        List<Integer> residentPriorities = new ArrayList<>();

        // The resident priorities list must have a size equal to the resident lists with 0 as default values.
        for (int i = 0; i < listSize; i++)
            residentPriorities.add(0);

        // Lower case all keywords.
        for (int i = 0; i < keywords.length; i++)
            keywords[i] = keywords[i].toLowerCase();

        boolean isExistingMatchFound = false;

        // For every keyword existing on a resident name, its priority will increase.
        for (int i = 0; i < listSize; i++) {
            String residentName = residentNames.get(i).toLowerCase();
            for (int j = 0; j < keywords.length; j++)
                if (residentName.contains(keywords[j].toLowerCase())) {
                    residentPriorities.set(i, residentPriorities.get(i) + 1);
                    isExistingMatchFound = true;
                }
        }

        // If all the priority levels are 0, then immediately return the resident lists with empty elements.
        if (!isExistingMatchFound)
            return new List[]{new ArrayList<>(), new ArrayList<>()};

        // Clone the lists.
        List<String> residentIdsTemp = new ArrayList<>();
        List<String> residentNamesTemp = new ArrayList<>();

        // Cloning...
        for(int i = 0; i < residentIds.size(); i++) {
            residentIdsTemp.add(residentIds.get(i));
            residentNamesTemp.add(residentNames.get(i));
        }

        // Remove the residents from the lists if their priority is 0.
        for (int i = 0; i < residentPriorities.size(); i++)
            if (residentPriorities.get(i) == 0) {
                residentPriorities.remove(i);
                residentIdsTemp.remove(i);
                residentNamesTemp.remove(i);

                i -= 1;
            }

        // Update list size to be equiavlent with the reduced residents list.
        listSize = residentPriorities.size();

        // Make a list storing the sorted resident IDs and names according to their priority level.
        // Residents with a priority level less than 1 will not be added in the list.
        List<String> newResidentIds = new ArrayList<>();
        List<String> newResidentNames = new ArrayList<>();

        // Use selection sorting to build the new resident lists.
        for (int i = 0; i < listSize - 1; i++) {
            // Determine the index of the resident with the highest priority.
            int highestPriorityIndex = 0;
            for (int j = 1; j < residentPriorities.size(); j++)
                if (residentPriorities.get(j) > residentPriorities.get(highestPriorityIndex))
                    highestPriorityIndex = j;

            // Add the resident with the highest priority to the new resident lists.
            newResidentIds.add(residentIdsTemp.get(highestPriorityIndex));
            newResidentNames.add(residentNamesTemp.get(highestPriorityIndex));

            // remove the resident with the highest priority from the original resident lists, so that
            // the highest priority resident will not be reiterated from the list and the next resident with the
            // highest priority will be determined.
            residentIdsTemp.remove(highestPriorityIndex);
            residentNamesTemp.remove(highestPriorityIndex);
            residentPriorities.remove(highestPriorityIndex);
        }

        // Only 1 element will remain inside the temporary resident lists - the resident with the lowest priority.
        // Transfer them to the new Resident lists.
        newResidentIds.add(residentIdsTemp.get(0));
        newResidentNames.add(residentNamesTemp.get(0));

        List[] lists = new List[]{newResidentIds, newResidentNames};

        return lists;
    }
}